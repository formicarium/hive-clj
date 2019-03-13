package hive_clj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.util.ThreadLocalScopeManager;

/**
 * HiveTracer makes it easy to test the semantics of OpenTracing instrumentation.
 *
 * By using a HiveTracer as an io.opentracing.Tracer implementation for unittests, a developer can assert that Span
 * properties and relationships with other Spans are defined as expected by instrumentation code.
 *
 * The MockTracerTest has simple usage examples.
 */
public class HiveTracer implements Tracer {
    private final List<HiveSpan> finishedSpans = new ArrayList<>();
    private final Propagator propagator;
    private final ScopeManager scopeManager;

    public HiveTracer() {
        this(new ThreadLocalScopeManager(), Propagator.TEXT_MAP);
    }

    public HiveTracer(ScopeManager scopeManager) {
        this(scopeManager, Propagator.TEXT_MAP);
    }

    public HiveTracer(ScopeManager scopeManager, Propagator propagator) {
        this.scopeManager = scopeManager;
        this.propagator = propagator;
    }

    /**
     * Create a new HiveTracer that passes through any calls to inject() and/or extract().
     */
    public HiveTracer(Propagator propagator) {
        this(new ThreadLocalScopeManager(), propagator);
    }

    /**
     * Clear the finishedSpans() queue.
     *
     * Note that this does *not* have any effect on Spans created by HiveTracer that have not finish()ed yet; those
     * will still be enqueued in finishedSpans() when they finish().
     */
    public synchronized void reset() {
        this.finishedSpans.clear();
    }

    /**
     * @return a copy of all finish()ed MockSpans started by this HiveTracer (since construction or the last call to
     * HiveTracer.reset()).
     *
     * @see HiveTracer#reset()
     */
    public synchronized List<HiveSpan> finishedSpans() {
        return new ArrayList<>(this.finishedSpans);
    }

    /**
     * Noop method called on {@link Span#finish()}.
     */
    protected void onSpanFinished(HiveSpan hiveSpan) {
    }

    /**
     * Propagator allows the developer to intercept and verify any calls to inject() and/or extract().
     *
     * By default, HiveTracer uses Propagator.PRINTER which simply logs such calls to System.out.
     *
     * @see HiveTracer#HiveTracer(Propagator)
     */
    public interface Propagator {
        <C> void inject(HiveSpan.MockContext ctx, Format<C> format, C carrier);
        <C> HiveSpan.MockContext extract(Format<C> format, C carrier);

        Propagator PRINTER = new Propagator() {
            @Override
            public <C> void inject(HiveSpan.MockContext ctx, Format<C> format, C carrier) {
                System.out.println("inject(" + ctx + ", " + format + ", " + carrier + ")");
            }

            @Override
            public <C> HiveSpan.MockContext extract(Format<C> format, C carrier) {
                System.out.println("extract(" + format + ", " + carrier + ")");
                return null;
            }
        };

        Propagator TEXT_MAP = new Propagator() {
            public static final String SPAN_ID_KEY = "spanid";
            public static final String TRACE_ID_KEY = "traceid";
            public static final String BAGGAGE_KEY_PREFIX = "baggage-";

            @Override
            public <C> void inject(HiveSpan.MockContext ctx, Format<C> format, C carrier) {
                if (carrier instanceof TextMap) {
                    TextMap textMap = (TextMap) carrier;
                    for (Map.Entry<String, String> entry : ctx.baggageItems()) {
                        textMap.put(BAGGAGE_KEY_PREFIX + entry.getKey(), entry.getValue());
                    }
                    textMap.put(SPAN_ID_KEY, String.valueOf(ctx.spanId()));
                    textMap.put(TRACE_ID_KEY, String.valueOf(ctx.traceId()));
                } else {
                    throw new IllegalArgumentException("Unknown carrier");
                }
            }

            @Override
            public <C> HiveSpan.MockContext extract(Format<C> format, C carrier) {
                Long traceId = null;
                Long spanId = null;
                Map<String, String> baggage = new HashMap<>();

                if (carrier instanceof TextMap) {
                    TextMap textMap = (TextMap) carrier;
                    for (Map.Entry<String, String> entry : textMap) {
                        if (TRACE_ID_KEY.equals(entry.getKey())) {
                            traceId = Long.valueOf(entry.getValue());
                        } else if (SPAN_ID_KEY.equals(entry.getKey())) {
                            spanId = Long.valueOf(entry.getValue());
                        } else if (entry.getKey().startsWith(BAGGAGE_KEY_PREFIX)){
                            String key = entry.getKey().substring((BAGGAGE_KEY_PREFIX.length()));
                            baggage.put(key, entry.getValue());
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Unknown carrier");
                }

                if (traceId != null && spanId != null) {
                    return new HiveSpan.MockContext(traceId, spanId, baggage);
                }

                return null;
            }
        };
    }

    @Override
    public ScopeManager scopeManager() {
        return this.scopeManager;
    }

    @Override
    public SpanBuilder buildSpan(String operationName) {
        return new SpanBuilder(operationName);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        this.propagator.inject((HiveSpan.MockContext)spanContext, format, carrier);
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {
        return this.propagator.extract(format, carrier);
    }

    @Override
    public Span activeSpan() {
        Scope scope = this.scopeManager.active();
        return scope == null ? null : scope.span();
    }

    synchronized void reportSpan(HiveSpan hiveSpan) {
        System.out.println("FOO");
        this.finishedSpans.add(hiveSpan);
        this.onSpanFinished(hiveSpan);
    }

    private SpanContext activeSpanContext() {
        Span span = activeSpan();
        if (span == null) {
            return null;
        }

        return span.context();
    }

    public final class SpanBuilder implements Tracer.SpanBuilder {
        private final String operationName;
        private long startMicros;
        private List<HiveSpan.Reference> references = new ArrayList<>();
        private boolean ignoringActiveSpan;
        private Map<String, Object> initialTags = new HashMap<>();

        SpanBuilder(String operationName) {
            this.operationName = operationName;
        }

        @Override
        public SpanBuilder asChildOf(SpanContext parent) {
            return addReference(References.CHILD_OF, parent);
        }

        @Override
        public SpanBuilder asChildOf(Span parent) {
            if (parent == null) {
                return this;
            }
            return addReference(References.CHILD_OF, parent.context());
        }

        @Override
        public SpanBuilder ignoreActiveSpan() {
            ignoringActiveSpan = true;
            return this;
        }

        @Override
        public SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
            if (referencedContext != null) {
                this.references.add(new HiveSpan.Reference((HiveSpan.MockContext) referencedContext, referenceType));
            }
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, String value) {
            this.initialTags.put(key, value);
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, boolean value) {
            this.initialTags.put(key, value);
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, Number value) {
            this.initialTags.put(key, value);
            return this;
        }

        @Override
        public SpanBuilder withStartTimestamp(long microseconds) {
            this.startMicros = microseconds;
            return this;
        }

        @Override
        public Scope startActive(boolean finishOnClose) {
            return HiveTracer.this.scopeManager().activate(this.startManual(), finishOnClose);
        }

        @Override
        public HiveSpan start() {
            return startManual();
        }

        @Override
        public HiveSpan startManual() {
            if (this.startMicros == 0) {
                this.startMicros = HiveSpan.nowMicros();
            }
            SpanContext activeSpanContext = activeSpanContext();
            if(references.isEmpty() && !ignoringActiveSpan && activeSpanContext != null) {
                references.add(new HiveSpan.Reference((HiveSpan.MockContext) activeSpanContext, References.CHILD_OF));
            }
            return new HiveSpan(HiveTracer.this, operationName, startMicros, initialTags, references);
        }
    }
}
