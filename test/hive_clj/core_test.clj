(ns hive-clj.core-test
  (:require [midje.sweet :refer :all]
            [hive-clj.adapters :as adapters]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [hive-clj.models :as models])
  (:import java.time.LocalDateTime))

(def cid "Shuffle_3fa149e.xlpDU.ZIKGH")
(def uri "some-uri")
(def message-map-sample {:headers {"X-Correlation-ID" cid}
                         :uri uri
                         :status 200
                         :request-method :get
                         :service "purgatory"
                         :server-port 8080})

(fact "We can get trace-id from cid"
  (adapters/cid->trace-id cid) => "Shuffle_3fa149e")

(fact "We can get parent-id from cid"
  (adapters/cid->parent-id cid) => "Shuffle_3fa149e.xlpDU")

(fact "We can get span-id from cid"
  (adapters/cid->span-id cid) => "Shuffle_3fa149e.xlpDU.ZIKGH")

(fact "We can build a span context from a message-map"
  (adapters/map->span-ctx message-map-sample)
  => {:trace-id "Shuffle_3fa149e"
      :parent-id "Shuffle_3fa149e.xlpDU"
      :span-id "Shuffle_3fa149e.xlpDU.ZIKGH"})

(fact "We can build trace-payload from message-map"
  (let [message-map message-map-sample]
    (adapters/trace-payload message-map)
    => (match (m/embeds {:timestamp #(instance? LocalDateTime %)
                         :payload   (str message-map-sample)
                         :tags    {:http  {:method      "get"
                                           :status_code 200
                                           :url         uri}
                                   :peer  {:service "purgatory"
                                           :port    8080}
                                   :kind  "server"
                                   :event "in-request"}
                         :context {:trace-id  "Shuffle_3fa149e"
                                   :parent-id "Shuffle_3fa149e.xlpDU"
                                   :span-id   "Shuffle_3fa149e.xlpDU.ZIKGH"}}))))

(fact "We can build span tags from message-map (in-request)"
  (let [message-map message-map-sample]
    (adapters/map->span-tags message-map-sample)
    => (match (m/equals {:http  {:method      "get"
                                 :status_code 200
                                 :url         uri}
                         :peer  {:service "purgatory"
                                 :port    8080}
                         :kind  "server"
                         :event "in-request"}))))
