(ns hive-clj.adapters
  (:require [cheshire.core :as cheshire])
  (:import java.time.LocalDateTime))

(defn str->bytes [str]
  (when str
    (.getBytes str)))

(defn raw->event [message-map]
  [(cheshire/generate-string (:meta message-map)) (cheshire/generate-string (:payload message-map))])

(defn map->span-tags [message-map]
  {:http {:method (name (:request-method message-map))
          :status_code (:status message-map)
          :url (:uri message-map)}
   :peer {:service (:service message-map)
          :port (:server-port message-map)}
   :kind "server"
   :event "in-request"})

(defn cid->trace-id [cid]
  (second (re-find #"(^[A-Za-z0-9\-\_]+)\." cid)))

(defn cid->parent-id [cid]
  (second (re-find #"^(.*)\." cid)))

(def cid->span-id identity)

(defn cid->span-ctx [cid]
  {:trace-id (cid->trace-id cid)
   :span-id (cid->span-id cid)
   :parent-id (cid->parent-id cid)})

(defn map->cid [{:keys [headers]}]
  (get headers "X-Correlation-ID"))

(defn map->span-ctx [message-map]
  (cid->span-ctx (map->cid message-map)))

(defn map->op-name [{:keys [uri]}]
  uri)

(defn trace-payload [message-map];; supposedly a pedestal request map, at least for now
  {:timestamp  (LocalDateTime/now)
   :tags (map->span-tags message-map)
   :payload (str message-map)
   :context (map->span-ctx message-map)})
