(ns hive-clj.adapters
  (:require [cheshire.core :as cheshire :refer :all]
            [cheshire.generate :refer [add-encoder]])
  (:import java.time.LocalDateTime))

(defn str->bytes [str]
  (when str
    (.getBytes str)))

(add-encoder java.time.LocalDateTime
             (fn [c jsonGenerator]
               (.writeString jsonGenerator (.toString c))))

(defn raw->event [message-map]
  [(cheshire/generate-string (:meta message-map)) (cheshire/generate-string (:payload message-map))])

(defn extract-service [{headers :headers}]
  (get headers "host"))

(def specific-tags {:out-request  {:direction  "producer"
                                   :type "out-request"}

                    :in-request   {:direction  "consumer"
                                   :type "in-request"}

                    :in-response  {:direction  "producer"
                                   :type "in-response"}

                    :out-response {:direction  "consumer"
                                   :type "out-response"}})

(defmulti map->span-tags :req-type)

(defmethod map->span-tags :in-request [{:keys [req-type request]}]
  (merge {:http {:method (name (:request-method request))
                 :url (:uri request)}
          :peer {:service (extract-service request)
                 :port (:server-port request)}}
         (req-type specific-tags)))

(defmethod map->span-tags :out-request [{:keys [req-type request]}]
  (merge {:http {:method (name (:request-method request))
                 :url (:uri request)}
          :peer {:service (extract-service request)
                 :port (:server-port request)}}
         (req-type specific-tags)))

(defmethod map->span-tags :in-response [{:keys [req-type request response]}]
  (merge {:http {:method (name (:request-method request))
                 :status_code (:status response)
                 :url (:uri request)}
          :peer {:service (extract-service request)
                 :port (:server-port request)}}
         (req-type specific-tags)))

(defmethod map->span-tags :out-response [{:keys [req-type request response]}]
  (merge {:http {:method (name (:request-method request))
                 :status_code (:status response)
                 :url (:uri request)}
          :peer {:service (extract-service request)
                 :port (:server-port request)}}
         (req-type specific-tags)))

(defn cid->trace-id [cid]
  (first (clojure.string/split cid #"\.")))

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

(defn extract-payload [message-map select-fn]
  (merge {:headers {}
          :body {}}
         (select-keys (select-fn message-map) [:headers :body])))

(defmulti trace-payload :req-type)

(defmethod trace-payload :in-request [message-map];; supposedly a pedestal request map, at least for now
  {:start  (LocalDateTime/now)
   :tags (map->span-tags message-map)
   :payload (extract-payload message-map :request)
   :context (map->span-ctx message-map)})

(defmethod trace-payload :out-request [message-map]
  {:start  (LocalDateTime/now)
   :tags (map->span-tags message-map)
   :payload (extract-payload message-map :request)
   :context (map->span-ctx message-map)})

(defmethod trace-payload :in-response [message-map]
  {:start (LocalDateTime/now)
   :end (LocalDateTime/now)
   :tags (map->span-tags message-map)
   :payload (extract-payload message-map :response)
   :context (map->span-ctx message-map)})

(defmethod trace-payload :out-response [message-map]
  {:start (LocalDateTime/now)
   :end (LocalDateTime/now)
   :tags (map->span-tags message-map)
   :payload (extract-payload message-map :response)
   :context (map->span-ctx message-map)})

(defn hive-message [message-map]
  (let [service (extract-service (:request message-map))]
    {:meta {:type :new-event
            :service (keyword service)}
     :identity service
     :payload (trace-payload message-map)}))
