(ns hive-clj.adapters
  (:require [cheshire.core :as cheshire])
  (:import java.time.LocalDateTime))

(defn str->bytes [str]
  (when str
    (.getBytes str)))

(defn raw->event [message-map]
  [(cheshire/generate-string (:meta message-map)) (cheshire/generate-string (:payload message-map))])

(defn extract-service [{headers :headers}]
  (get headers "host"))

(def specific-tags {:in-request   {:kind  "server"
                                   :event "in-request"}
                    :out-response {:kind  "client"
                                   :event "out-response"}
                    :out-request  {:kind  "client"
                                   :event "out-request"}
                    :in-response  {:kind  "server"
                                   :event "in-response"}})

(defmulti map->span-tags :req-type)

(defmethod map->span-tags :in-request [{:keys [req-type request]}]
  (merge {:http {:method (name (:request-method request))
                 :status_code (:status request)
                 :url (:uri request)}
          :peer {:service (extract-service request)
                 :port (:server-port request)}}
         (req-type specific-tags)))

(defmethod map->span-tags :out-request [{:keys [req-type request]}]
  (merge {:http {:method (name (:request-method request))
                 :status_code (:status request)
                 :url (:uri request)}
          :peer {:service (extract-service request)
                 :port (:server-port request)}}
         (req-type specific-tags)))

(defn cid->trace-id [cid]
  (second (re-find #"(^[A-Za-z0-9\-\_]+)\." cid)))

(defn cid->parent-id [cid]
  (second (re-find #"^(.*)\." cid)))

(def cid->span-id identity)

(defn cid->span-ctx [cid]
  {:trace-id (cid->trace-id cid)
   :span-id (cid->span-id cid)
   :parent-id (cid->parent-id cid)})

(defn request->cid [{:keys [headers]}]
  (get headers "X-Correlation-ID"))

(defn map->span-ctx [{:keys [request] :as message-map}]
  (cid->span-ctx (request->cid request)))

(defn map->op-name [{:keys [uri]}]
  uri)

(defmulti trace-payload :req-type)

(defmethod trace-payload :in-request [message-map];; supposedly a pedestal request map, at least for now
  {:timestamp  (LocalDateTime/now)
   :tags (map->span-tags message-map)
   :payload (str message-map)
   :context (map->span-ctx message-map)})

(defmethod trace-payload :out-request [message-map]
  {:timestamp  (LocalDateTime/now)
   :tags (map->span-tags message-map)
   :payload (str message-map)
   :context (map->span-ctx message-map)})
