(ns hive-clj.tracer
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.log :as log])
  (:import [hive_clj HiveTracer]))

(comment
 (def tracer (HiveTracer.))
 (log/-register tracer)
 (log/log [:bizzniss "1111"])


 log/default-tracer

 )
