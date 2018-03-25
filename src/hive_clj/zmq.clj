(ns hive-clj.zmq
  (:require [zeromq.zmq :as zmq]
            [hive-clj.config :as config]
            [hive-clj.adapters :as adapters]
            [com.stuartsierra.component :as component]
            [clojure.core.async :as async])
  (:import java.time.LocalDateTime))

(def context (zmq/context 1))

(defn new-dealer-socket! [endpoint ident]
  (let [dealer (zmq/socket context :dealer)]
    (zmq/set-identity dealer (adapters/str->bytes ident))
    (zmq/set-receive-timeout dealer config/zmq-receive-timeout-ms)
    (zmq/set-send-timeout dealer config/zmq-send-timeout)
    (zmq/connect dealer endpoint)))

(defn send! [socket & parts]
  (loop [[x & xs] parts]
    (when x
      (if xs
        (do (zmq/send socket (adapters/str->bytes x) zmq/send-more)
            (recur xs))
        (zmq/send socket (adapters/str->bytes x))))))

(defonce last-ack (atom (LocalDateTime/now)))

(defn handle-ack-received []
  (swap! last-ack (constantly (LocalDateTime/now))))

(defn handle-ack-not-received []
  (when (.isBefore @last-ack (.minusSeconds (LocalDateTime/now) 10)))
  (prn "hive died"))

(defn await-ack [dealer]
  (if-let [ack (zmq/receive-str dealer)];;TODO- kill this thread immediatly if the terminate-hive-client! is triggered and this is running
    (handle-ack-received)
    (handle-ack-not-received)))

(defn send-dealer-message! [dealer message-map]
  (apply send! dealer (adapters/raw->event message-map))
  (await-ack dealer))

(defn heartbeat-msg [payload]
  {:payload payload :meta {:type :heartbeat}})

(defn send-channel [dealer heartbeat-timing-ms]
  (let [ch (async/chan config/main-channel-buffer-size)
        stop-ch (async/chan)]
    (async/go-loop []
      (when-let [value (async/<! ch)]
        (some->> value (send-dealer-message! dealer))
        (recur)))
    (async/go-loop []
      (when (async/alt! stop-ch false (async/timeout heartbeat-timing-ms) :keep-going)
        (async/>! ch (heartbeat-msg ""))
        (recur)))
    {:main ch
     :heartbeat stop-ch}))

(defn terminate-sender-channel! [{:keys [main heartbeat]}]
  (async/close! main)
  (async/reduce (constantly nil) [] main)
  (async/close! heartbeat))

(defn terminate-dealer-socket! [dealer] (zmq/close dealer))

(defprotocol ZMQDealer
  (send-message! [this message-map]))

(defrecord HiveClient [endpoint ident]
  component/Lifecycle
  (start [this]
    (let [dealer (new-dealer-socket! endpoint ident)]
      (assoc this :dealer dealer
             :channel (send-channel dealer config/heartbeat-timing-ms))))

  (stop [this]
    (terminate-sender-channel! (:channel this))
    (terminate-dealer-socket! (:dealer this))
    (dissoc this :channel :dealer))

  ZMQDealer
  (send-message! [this message-map]
    (async/go (async/>! (:channel this) message-map))))

(defn new-hive-client! [endpoint ident]
  (component/start (->HiveClient endpoint ident)))

(defn terminate-client! [client]
  (component/stop client))
