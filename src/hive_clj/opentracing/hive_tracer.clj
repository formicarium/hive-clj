(ns hive-clj.opentracing.hive-tracer
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.log :as log])
  (:import [clojure.lang IDeref]
           [io.opentracing Tracer SpanContext Tracer$SpanBuilder Span]))

(defn thread-local [init]
  (let [generator (proxy [ThreadLocal] [] (initialValue [] init))]
    (reify IDeref
      (deref [this]
        (.get generator)))))

(defrecord HiveSpanBuilder []

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
  (scopeManager [this])
  (activeSpan [this]
    @@(:active-span this))(buildSpan [this opeation-name])
  (inject [this context format carrier])
  (extract [this format adapter]
    (let [headers-map (into {} (iterator-seq (.iterator ^Iterable adapter)))
          correlation-id (get headers-map "hive-correlation-id")]
      (when correlation-id
        (->HiveSpanContext correlation-id)))))

(defn new-hive-tracer []
  (map->HiveTracer {}))

(comment
  (log/-register (new-hive-tracer))
  (log/log :foo "42"))
