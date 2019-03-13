(ns hive-clj.tracer
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.log :as log])
  (:import [clojure.lang IDeref]
           [io.opentracing Tracer SpanContext Tracer$SpanBuilder Span]))
