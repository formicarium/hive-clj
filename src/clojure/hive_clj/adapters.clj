(ns hive-clj.adapters
  (:require [cheshire.core :as cheshire]))

(defn str->bytes [str]
  (when str
    (.getBytes str)))

(defn raw->event [message-map]
  [(cheshire/generate-string (:meta message-map)) (cheshire/generate-string (:payload message-map))])
