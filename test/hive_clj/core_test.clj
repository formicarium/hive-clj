(ns hive-clj.core-test
  (:require [midje.sweet :refer :all]
            [hive-clj.adapters :as adapters]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [hive-clj.models :as models])
  (:import java.time.LocalDateTime))

(def cid "Shuffle_3fa149e.xlpDU.ZIKGH")

(fact "We can get trace-id from cid"
  (adapters/cid->trace-id cid) => "Shuffle_3fa149e")

(fact "We can get parent-id from cid"
  (adapters/cid->parent-id cid) => "Shuffle_3fa149e.xlpDU")

(fact "We can get span-id from cid"
  (adapters/cid->span-id cid) => "Shuffle_3fa149e.xlpDU.ZIKGH")

(fact "We can build a span context from a message-map"
  (adapters/map->span-ctx {:cid cid})
  => {:trace-id "Shuffle_3fa149e"
      :parent-id "Shuffle_3fa149e.xlpDU"
      :span-id "Shuffle_3fa149e.xlpDU.ZIKGH"})

(fact "We can build trace-payload from message-map"
  (let [message-map {:cid cid}]
    (adapters/trace-payload message-map)
    => (match (m/embeds {:op-name string?
                         :start #(instance? LocalDateTime %)
                         :context {:trace-id "Shuffle_3fa149e"
                                   :parent-id "Shuffle_3fa149e.xlpDU"
                                   :span-id "Shuffle_3fa149e.xlpDU.ZIKGH"}}))))
