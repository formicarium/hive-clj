(ns hive-clj.models
  (:require [cheshire.core :as cheshire]
            [schema.core :as s])
  (:import java.time.LocalDateTime))

(s/defschema SpanTags {:component s/Str;; The software package, framework, library, or module that generated the associated Span. E.g., "grpc", "django", "JDBI"
                       :db.instance s/Str;; Database instance name. E.g., In java, if the jdbc.url="jdbc:mysql://127.0.0.1:3306/customers", the instance name is "customers"
                       :db.statement s/Str;; A database statement for the given database type. E.g., for db.type="sql", "SELECT * FROM wuser_table"; for db.type="redis", "SET mykey 'WuValue'"
                       :db.type s/Str;; Database type. For any SQL database, "sql". For others, the lower-case database category, e.g. "cassandra", "hbase", or "redis"
                       :db.user s/Str;; Username for accessing database. E.g., "readonly_user" or "reporting_user"
                       :error s/Bool;; true if and only if the application considers the operation represented by the Span to have failed
                       :http.method s/Str;; HTTP method of the request for the associated Span. E.g., "GET", "POST"
                       :http.status_code s/Int;; HTTP response status code for the associated Span. E.g., 200, 503, 404
                       :http.url s/Str;; URL of the request being handled in this segment of the trace, in standard URI format. E.g., "https://domain.net/path/to?resource=here"
                       :message_bus.destination s/Str;; An address at which messages can be exchanged. E.g. A Kafka record has an associated "topic name" that can be extracted by the instrumented producer or consumer and stored using this tag.
                       :peer.address s/Str;; Remote "address", suitable for use in a networking client library. This may be a "ip:port", a bare "hostname", a FQDN, or even a JDBC substring like "mysql://prod-db:3306"
                       :peer.hostname s/Str;; Remote hostname. E.g., "opentracing.io", "internal.dns.name"
                       :peer.ipv4 s/Str;; Remote IPv4 address as a .-separated tuple. E.g., "127.0.0.1"
                       :peer.ipv6 s/Str;; Remote IPv6 address as a string of colon-separated 4-char hex tuples. E.g., "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
                       :peer.port s/Int;; Remote port. E.g., 80
                       :peer.service s/Str;; Remote service name (for some unspecified definition of "service"). E.g., "elasticsearch", "a_custom_microservice", "memcache". Meaning should correspond with values set in service
                       :sampling.priority s/Int;; If greater than 0, a hint to the Tracer to do its best to capture the trace. If 0, a hint to the trace to not-capture the trace. If absent, the Tracer should use its default sampling mechanism
                       :service s/Str;; The service name for a span, which overrides any default "service name" property defined in a tracer's config. The meaning of service should correspond to the value set in peer.service, except it is applied to the current span. This tag is meant to only be used when a tracer is reporting spans on behalf of another service (for example, a service mesh reporting on behalf of the services it is proxying, or an out-of-band reporter which reads in log files). This tag does not need to be used when reporting spans for the service the tracer is running in
                       :span.kind s/Str;; Either "client" or "server" for the appropriate roles in an RPC, and "producer" or "consumer" for the appropriate roles in a messaging scenario
                       })

(s/defschema SpanLog {:error.kind s/Str;; The type or "kind" of an error (only for event="error" logs). E.g., "Exception", "OSError"
                      :error.object s/Str;; For languages that support such a thing (e.g., Java, Python), the actual Throwable/Exception/Error object instance itself. E.g., A java.lang.UnsupportedOperationException instance, a python exceptions.NameError instance
                      :event s/Str;; A stable identifier for some notable moment in the lifetime of a Span. For instance, a mutex lock acquisition or release or the sorts of lifetime events in a browser page load described in the Performance.timing specification. E.g., from Zipkin, "cs", "sr", "ss", or "cr". Or, more generally, "initialized" or "timed out". For errors, "error"
                      :message s/Str;; A concise, human-readable, one-line message explaining the event. E.g., "Could not connect to backend", "Cache invalidation succeeded"
                      :stack s/Str;; A stack trace in platform-conventional format; may or may not pertain to an error. E.g., "File \"example.py\", line 7, in \<module\>\ncaller()\nFile \"example.py\", line 5, in caller\ncallee()\nFile \"example.py\", line 2, in callee\nraise Exception(\"Yikes\")\n"
                      :payload s/Str;; To send raw request/response/kafka-message data for debugging (not in the official spec)
                      })
(s/defschema SpanLogs {LocalDateTime SpanLog})

(s/defschema SpanContext {:trace-id s/Str
                          :span-id s/Str
                          :parent-id s/Str})

(s/defschema BaggageItems {s/Any s/Any})

(s/defschema Span {:op-name s/Str
                   :start  LocalDateTime
                   :finish LocalDateTime
                   :tags SpanTags
                   :logs SpanLogs
                   :context SpanContext
                   :baggage-items BaggageItems})
