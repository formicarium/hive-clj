(ns hive-clj.tracer
  (:require [com.stuartsierra.component :as component])
  (:import [clojure.lang IDeref]
           [io.opentracing Tracer SpanContext Tracer$SpanBuilder]))

(defn thread-local [init]
  (let [generator (proxy [ThreadLocal] [] (initialValue [] init))]
    (reify IDeref
      (deref [this]
        (.get generator)))))

(defrecord HiveSpanBuilder []
  component/Lifecycle
  (start [this]
    )

  (stop [this]
    )

  Tracer$SpanBuilder

  (ignoreActiveSpan [this]
    ))

(defrecord HiveSpanContext [correlation-id]
  SpanContext
  (baggageItems [this]
    {"parentId" correlation-id}))

(defrecord HiveTracer []
  component/Lifecycle
  (start [this]
    (assoc this :active-span (thread-local (atom nil))))

  (stop [this]
    (dissoc this :active-span))

  Tracer
  (activeSpan [this]
    @@(:active-span this))

  (extract [this format adapter]
    (let [headers-map    (into {} (iterator-seq (.iterator ^Iterable adapter)))
          correlation-id (get headers-map "hive-correlation-id")]
      (when correlation-id
        (->HiveSpanContext correlation-id)))))
