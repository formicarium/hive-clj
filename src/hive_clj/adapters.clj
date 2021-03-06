(ns hive-clj.adapters
  (:require [cheshire.core :as cheshire :refer :all]
            [cheshire.generate :refer [add-encoder]]
            [clojure.java.data :refer [from-java]]
            [hive-clj.models :as models]
            [schema.core :as s])
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

(def specific-tags {:out-request {:direction :producer
                                  :kind      :start
                                  :type      :http-out}

                    :in-request {:direction :consumer
                                 :kind      :start
                                 :type      :http-in}

                    :in-response {:direction :producer
                                  :kind      :end
                                  :type      :http-in}

                    :out-response {:direction :consumer
                                   :kind      :end
                                   :type      :http-out}})

(defmulti map->span-tags :req-type)

(s/defmethod map->span-tags :in-request :- models/SpanTags
  [{:keys [req-type request]}]
  (merge {:http {:method (name (:request-method request))
                 :url (:uri request)}
          :peer {:service (extract-service request)
                 :port (:server-port request)}}
         (req-type specific-tags)))

(s/defmethod map->span-tags :out-request :- models/SpanTags
  [{:keys [req-type request]}]
  (merge {:http {:method (name (:request-method request))
                 :url (:uri request)}
          :peer {:service (extract-service request)
                 :port (:server-port request)}}
         (req-type specific-tags)))

(s/defmethod map->span-tags :in-response :- models/SpanTags
  [{:keys [req-type request response]}]
  (merge {:http {:method (name (:request-method request))
                 :status_code (:status response)
                 :url (:uri request)}
          :peer {:service (extract-service request)
                 :port (:server-port request)}}
         (req-type specific-tags)))

(s/defmethod map->span-tags :out-response :- models/SpanTags
  [{:keys [req-type request response]}]
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

(s/defn cid->span-ctx :- models/SpanContext [cid :- s/Str]
  {:trace-id (cid->trace-id cid)
   :span-id (cid->span-id cid)
   :parent-id (cid->parent-id cid)})

(s/defn map->span-ctx :- models/SpanContext
  [{:keys [cid] :as message-map}]
  (cid->span-ctx cid))

(defn extract-payload [message-map select-fn]
  (-> (select-keys (select-fn message-map) [:headers :body])
      (update :headers #(or (from-java %) {}))
      (update :body #(or (from-java %) {}))))

(defmulti trace-payload :req-type)

(defmethod trace-payload :in-request [message-map];; supposedly a pedestal request map, at least for now
  {:timestamp  (LocalDateTime/now)
   :tags (map->span-tags message-map)
   :payload (extract-payload message-map :request)
   :context (map->span-ctx message-map)})

(defmethod trace-payload :out-request [message-map]
  {:timestamp  (LocalDateTime/now)
   :tags (map->span-tags message-map)
   :payload (extract-payload message-map :request)
   :context (map->span-ctx message-map)})

(defmethod trace-payload :in-response [message-map]
  {:timestamp (LocalDateTime/now)
   :tags (map->span-tags message-map)
   :payload (extract-payload message-map :response)
   :context (map->span-ctx message-map)})

(defmethod trace-payload :out-response [message-map]
  {:timestamp (LocalDateTime/now)
   :tags (map->span-tags message-map)
   :payload (extract-payload message-map :response)
   :context (map->span-ctx message-map)})


(defmulti hive-message #(-> % :meta :type))

(s/defmethod hive-message :heartbeat [message-map]
  message-map)

(s/defmethod hive-message :test [message-map]
  message-map)

(s/defmethod hive-message :new-event
  [message-map]
  (update-in message-map [:payload :payload] from-java))
