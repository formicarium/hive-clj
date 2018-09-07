(ns hive-clj.core-test
  (:require [hive-clj.adapters :as adapters]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [midje.sweet :refer :all]
            [schema.core :as s])
  (:import java.time.LocalDateTime))

(def cid "Shuffle_3fa149e.xlpDU.ZIKGH")
(def uri "some-uri")
(def message-map-sample {:request {:headers        {"X-Correlation-ID" cid
                                                    "host"             "purgatory"}
                                   :uri            uri
                                   :status         200
                                   :request-method :get
                                   :service        "purgatory"
                                   :server-port    8080}})

(defn req-sample [map req-type]
  (merge map {:req-type req-type}))

(def http-req-sample (partial req-sample message-map-sample))
(def in-request-sample (http-req-sample :in-request))
(def out-request-sample (http-req-sample :out-request))
(def in-response-sample (http-req-sample :in-response))
(def out-response-sample (http-req-sample :out-response))

(fact "We can get trace-id from cid"
  (adapters/cid->trace-id cid) => "Shuffle_3fa149e")

(fact "We can get parent-id from cid"
  (adapters/cid->parent-id cid) => "Shuffle_3fa149e.xlpDU")

(fact "We can get span-id from cid"
  (adapters/cid->span-id cid) => "Shuffle_3fa149e.xlpDU.ZIKGH")

(fact "We can build a span context from a message-map"
  (adapters/map->span-ctx message-map-sample)
  => {:trace-id  "Shuffle_3fa149e"
      :parent-id "Shuffle_3fa149e.xlpDU"
      :span-id   "Shuffle_3fa149e.xlpDU.ZIKGH"})


(fact "We can build span tags from message-map (in-request)"
  (let [message-map in-request-sample]
    (adapters/map->span-tags message-map)
    => (match (m/equals {:http      {:method      "get"
                                     :status_code 200
                                     :url         uri}
                         :peer      {:service "purgatory"
                                     :port    8080}
                         :direction "consumer"
                         :type      "in-request"}))))

(fact "We can build trace-payload from message-map for in-request"
  (let [message-map in-request-sample]
    (adapters/trace-payload message-map)
    => (match (m/embeds {:start   #(instance? LocalDateTime %)
                         :payload (str (assoc message-map-sample :req-type :in-request))
                         :tags    {:http      {:method      "get"
                                               :status_code 200
                                               :url         uri}
                                   :peer      {:service "purgatory"
                                               :port    8080}
                                   :direction "consumer"
                                   :type      "in-request"}
                         :context {:trace-id  "Shuffle_3fa149e"
                                   :parent-id "Shuffle_3fa149e.xlpDU"
                                   :span-id   "Shuffle_3fa149e.xlpDU.ZIKGH"}}))))

(fact "We can build trace-payload from message-map for out-request"
  (let [message-map out-request-sample]
    (adapters/trace-payload message-map)
    => (match (m/embeds {:start   #(instance? LocalDateTime %)
                         :payload (str (assoc message-map-sample :req-type :out-request))
                         :tags    {:http      {:method      "get"
                                               :status_code 200
                                               :url         uri}
                                   :peer      {:service "purgatory"
                                               :port    8080}
                                   :type      "out-request"
                                   :direction "producer"}
                         :context {:trace-id  "Shuffle_3fa149e"
                                   :parent-id "Shuffle_3fa149e.xlpDU"
                                   :span-id   "Shuffle_3fa149e.xlpDU.ZIKGH"}}))))

(fact "We can build trace-payload from message-map for in-response"
  (let [message-map in-response-sample]
    (adapters/trace-payload message-map)
    => (match (m/embeds {:start   #(instance? LocalDateTime %)
                         :end #(instance? LocalDateTime %)
                         :payload (str (assoc message-map-sample :req-type :in-response))
                         :tags    {:http      {:method      "get"
                                               :status_code 200
                                               :url         uri}
                                   :peer      {:service "purgatory"
                                               :port    8080}
                                   :type      "in-response"
                                   :direction "producer"}
                         :context {:trace-id  "Shuffle_3fa149e"
                                   :parent-id "Shuffle_3fa149e.xlpDU"
                                   :span-id   "Shuffle_3fa149e.xlpDU.ZIKGH"}}))))

(fact "We can build trace-payload from message-map for out-response"
  (let [message-map out-response-sample]
    (adapters/trace-payload message-map)
    => (match (m/embeds {:start   #(instance? LocalDateTime %)
                         :end #(instance? LocalDateTime %)
                         :payload (str (assoc message-map-sample :req-type :out-response))
                         :tags    {:http      {:method      "get"
                                               :status_code 200
                                               :url         uri}
                                   :peer      {:service "purgatory"
                                               :port    8080}
                                   :type      "out-response"
                                   :direction "consumer"}
                         :context {:trace-id  "Shuffle_3fa149e"
                                   :parent-id "Shuffle_3fa149e.xlpDU"
                                   :span-id   "Shuffle_3fa149e.xlpDU.ZIKGH"}}))))

(s/with-fn-validation
  (fact "We can build a hive message from message-map for in-request"
    (let [message-map in-request-sample]
      (adapters/hive-message message-map)
      => (match (m/embeds
                 {:meta     {:type    :new-event
                             :service :purgatory}
                  :identity "purgatory"
                  :payload  {:start   #(instance? LocalDateTime %)
                             :payload (str (assoc message-map-sample :req-type :in-request))
                             :tags    {:http      {:method      "get"
                                                   :status_code 200
                                                   :url         uri}
                                       :peer      {:service "purgatory"
                                                   :port    8080}
                                       :type      "in-request"
                                       :direction "consumer"}
                             :context {:trace-id  "Shuffle_3fa149e"
                                       :parent-id "Shuffle_3fa149e.xlpDU"
                                       :span-id   "Shuffle_3fa149e.xlpDU.ZIKGH"}}})))))
