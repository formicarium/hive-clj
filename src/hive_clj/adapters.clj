(ns hive-clj.adapters
  (:require [cheshire.core :as cheshire])
  (:import java.time.LocalDateTime))

(defn str->bytes [str]
  (when str
    (.getBytes str)))

(defn raw->event [message-map]
  [(cheshire/generate-string (:meta message-map)) (cheshire/generate-string (:payload message-map))])

(defn map->span-tags [message-map]
  ;; TODO implementation
  )

(defn map->span-logs [message-map]
  ;;TODO implementation
  )

(defn cid->trace-id [cid]
  (second (re-find #"(^[A-Za-z0-9\-\_]+)\." cid)))

(defn cid->parent-id [cid]
  (second (re-find #"^(.*)\." cid)))

(def cid->span-id identity)

(defn cid->span-ctx [cid]
  {:trace-id (cid->trace-id cid)
   :span-id (cid->span-id cid)
   :parent-id (cid->parent-id cid)})

(defn map->span-ctx [message-map]
 (cid->span-ctx (:cid message-map)))

(defn trace-payload [message-map]
  {:op-name "pimba";this would be cool to be the same keyword used in discovery endpoints
   :start  (LocalDateTime/now)
   :finish (LocalDateTime/now);TODO implement finish of span
   :tags (map->span-tags message-map)
   :logs (map->span-logs message-map)
   :context (map->span-ctx message-map)})
