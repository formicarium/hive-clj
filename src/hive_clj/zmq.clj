(ns hive-clj.zmq
  (:require [zeromq.zmq :as zmq]
            [cheshire.core :as cheshire]
            [com.stuartsierra.component :as component]
            [clojure.core.async :as async]))

(def context (zmq/context 1))

(defn str->bytes [str]
  (when str
    (.getBytes str)))

(defn new-dealer-socket! [endpoint ident]
  (let [dealer (zmq/socket context :dealer)]
    (zmq/set-identity dealer (str->bytes ident))
    (zmq/set-receive-timeout dealer 10000)
    (zmq/set-send-timeout dealer 1000)
    (zmq/connect dealer endpoint)))

(defn send! [socket & parts]
  (loop [[x & xs] parts]
    (when x
      (if xs
        (do (zmq/send socket (str->bytes x) zmq/send-more)
            (recur xs))
        (zmq/send socket (str->bytes x))))))

(defn send-dealer-message! [dealer message-map]
  (send! dealer (cheshire/generate-string (:meta message-map)) (cheshire/generate-string (:payload message-map))))

(defn send-channel [dealer]
  (let [ch (async/chan 1000)]
    (async/go-loop []
      (when-let [value (async/<! ch)]
        (some->> value (send-dealer-message! dealer))
        (recur)))
    ch))

(defn terminate-sender-channel! [ch] (async/close! ch))
(defn terminate-dealer-socket! [dealer] (zmq/close dealer))

(defprotocol ZMQDealer
  (send-message! [this message-map]))

(defrecord HiveClient [endpoint ident]
  component/Lifecycle
  (start [this]
    (let [dealer (new-dealer-socket! endpoint ident)]
      (assoc this :dealer dealer
                  :channel (send-channel dealer))))

  (stop [this]
    (terminate-sender-channel! (:channel this))
    (terminate-dealer-socket! (:dealer this))
    (dissoc this :channel :dealer))

  ZMQDealer
  (send-message! [this message-map]
    (send-dealer-message! (:dealer this) message-map)))

(defn new-hive-client! [endpoint ident]
  (component/start (->HiveClient endpoint ident)))

(defn terminate-client! [client]
  (component/stop client))
