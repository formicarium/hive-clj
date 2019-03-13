(ns hive-clj.opentracing.hive-span
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.log :as log])
  (:import [clojure.lang IDeref]
           [io.opentracing Tracer SpanContext Tracer$SpanBuilder Span]))



(defn now-timestamp []
  (System/currentTimeMillis))

(defn assoc-in-logs [{:keys [logs] :as hive-span} time new-logs]
  (assoc hive-span :logs (concat logs
                                 (map (fn [[k v]] {:timestamp time
                                                   :log/key k
                                                   :log/value v}) new-logs))))

(defn assoc-in-baggage [{:keys [baggage] :as hive-span}
                        new-key new-value] (assoc hive-span :baggage (assoc baggage new-key new-value)))

(defrecord HiveSpan [context parent-id logs baggage]
  Span
  (context [_] context)
  (setTag [this var1 var2] (assoc this var1 var2))
  (log [this map-or-str] (assoc-in-logs this (now-timestamp) (if (map? map-or-str) map-or-str {:str map-or-str})))
  (log [this time map-or-str] (assoc-in-logs this time (if (map? map-or-str) map-or-str {:str map-or-str})))
  (setBaggageItem [this str1 str2] (assoc-in-baggage this str1 str2))
  (getBaggageItem [this str] (get baggage str))
  (setOperationName [this op-name] (assoc this :operation-name op-name))
  (finish [this] (prn this))
  (finish [this time] (prn this time)))
