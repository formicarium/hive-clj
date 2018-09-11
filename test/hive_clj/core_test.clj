(ns hive-clj.core-test
  (:require [hive-clj.adapters :as adapters]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [midje.sweet :refer :all]
            [schema.core :as s])
  (:import java.time.LocalDateTime))

(def cid "Shuffle_3fa149e.xlpDU.ZIKGH")
(def uri "some-uri")
(def message-map-sample {:cid      cid
                         :request  {:headers        {"host"             "purgatory"}
                                    :uri            uri
                                    :body           {:some   "form"
                                                     :params "for us"}
                                    :request-method :get
                                    :service        "purgatory"
                                    :server-port    8080}
                         :response {:status  200
                                    :body    {:risco-quote "BELESSA"}
                                    :headers {"Content-Length"   "345"
                                              "Content-Encoding" "gzip"}}})

(defn req-sample [map req-type]
  (merge map {:req-type req-type}))

(def http-req-sample (partial req-sample message-map-sample))
(def in-request-sample (http-req-sample :in-request))
(def out-request-sample (http-req-sample :out-request))
(def in-response-sample (http-req-sample :in-response))
(def out-response-sample (http-req-sample :out-response))

(s/with-fn-validation
  (tabular
    (facts "About span context"
      (fact "We build trace-id"
        (adapters/cid->trace-id ?cid) => ?trace-id)

      (fact "We build parent-id"
        (adapters/cid->parent-id ?cid) => ?parent-id)

      (fact "We build span-id"
        (adapters/cid->span-id ?cid) => ?span-id))
    ?cid                           ?trace-id         ?parent-id              ?span-id
    "Shuffle_3fa149e.xlpDU.ZIKGH"  "Shuffle_3fa149e" "Shuffle_3fa149e.xlpDU" "Shuffle_3fa149e.xlpDU.ZIKGH"
    "A.B"                          "A"               "A"                     "A.B"
    "A"                            "A"                nil                    "A"))

(s/with-fn-validation
  (fact "We can build a span context from a message-map"
    (adapters/map->span-ctx message-map-sample)
    => {:trace-id  "Shuffle_3fa149e"
        :parent-id "Shuffle_3fa149e.xlpDU"
        :span-id   "Shuffle_3fa149e.xlpDU.ZIKGH"}))

(s/with-fn-validation
  (fact "We can build span tags from message-map (in-request)"
    (let [message-map in-request-sample]
      (adapters/map->span-tags message-map)
      => (match (m/equals {:http      {:method "get"
                                       :url    uri}
                           :peer      {:service "purgatory"
                                       :port    8080}
                           :direction :consumer
                           :kind      :start
                           :type      :http-in})))))

(s/with-fn-validation
  (tabular
    (fact "About extracting payload from message-map"
      (adapters/extract-payload ?map ?fn) => ?payload)
    ?map ?fn ?payload
    message-map-sample
    :request
    {:headers {"host" "purgatory"}
     :body    {:some   "form"
               :params "for us"}}

    message-map-sample
    :response
    {:body    {:risco-quote "BELESSA"}
     :headers {"Content-Length"   "345"
               "Content-Encoding" "gzip"}}

    {:request {:headers {"some" "headers"}
               :body    {}}}
    :request
    {:headers {"some" "headers"}
     :body    {}}

    {}
    :request
    {:headers {}
     :body    {}}))

(s/with-fn-validation
  (fact "We can build trace-payload from message-map for in-request"
    (let [message-map in-request-sample]
      (adapters/trace-payload message-map)
      => (match (m/embeds {:timestamp   #(instance? LocalDateTime %)
                           :payload (partial contains [:headers :body])
                           :tags    {:http      {:method      "get"
                                                 :url         uri}
                                     :peer      {:service "purgatory"
                                                 :port    8080}
                                     :direction :consumer
                                     :kind      :start
                                     :type      :http-in}
                           :context (every-pred seq map?)})))))

(s/with-fn-validation
  (fact "We can build trace-payload from message-map for out-request"
    (let [message-map out-request-sample]
      (adapters/trace-payload message-map)
      => (match (m/embeds {:timestamp #(instance? LocalDateTime %)
                           :payload   (partial contains [:headers :body])
                           :tags      {:http      {:method "get"
                                                   :url    uri}
                                       :peer      {:service "purgatory"
                                                   :port    8080}
                                       :type      :http-out
                                       :kind      :start
                                       :direction :producer}
                           :context   (every-pred seq map?)})))))

(s/with-fn-validation
  (fact "We can build trace-payload from message-map for in-response"
    (let [message-map in-response-sample]
      (adapters/trace-payload message-map)
      => (match (m/embeds {:timestamp #(instance? LocalDateTime %)
                           :payload   (partial contains [:headers :body])
                           :tags      {:http      {:method      "get"
                                                   :status_code 200
                                                   :url         uri}
                                       :peer      {:service "purgatory"
                                                   :port    8080}
                                       :type      :http-in
                                       :kind      :end
                                       :direction :producer}
                           :context   (every-pred seq map?)})))))

(s/with-fn-validation
  (fact "We can build trace-payload from message-map for out-response"
    (let [message-map out-response-sample]
      (adapters/trace-payload message-map)
      => (match (m/embeds {:timestamp #(instance? LocalDateTime %)
                           :payload   (partial contains [:headers :body])
                           :tags      {:http      {:method      "get"
                                                   :status_code 200
                                                   :url         uri}
                                       :peer      {:service "purgatory"
                                                   :port    8080}
                                       :type      :http-out
                                       :kind      :end
                                       :direction :consumer}
                           :context   (every-pred seq map?)})))))

(s/with-fn-validation
  (fact "We can build a hive message from message-map for in-request"
    (let [message-map in-request-sample]
      (adapters/hive-message message-map)
      => (match (m/embeds
                 {:meta     {:type    :new-event
                             :service :purgatory}
                  :identity "purgatory"
                  :payload  map?})))))
