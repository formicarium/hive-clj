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
    ;(zmq/set-send-timeout dealer config/zmq-send-timeout)
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
  (when (.isBefore @last-ack (.minusSeconds (LocalDateTime/now) config/hive-unresponsive-threshold-s))
    (prn "hive died")))

(defn await-ack [dealer]
  (if-let [ack (zmq/receive-str dealer)];;TODO- kill this thread immediatly if the terminate-hive-client! is triggered and this is running
    (handle-ack-received)
    (handle-ack-not-received)))

(defn send-dealer-message! [dealer message-map]
  (apply send! dealer (adapters/raw->event message-map))
  (await-ack dealer))

(defn heartbeat-msg [client]
  {:payload "PING"
   :meta {:type :heartbeat
          :service (:ident client)}})

(defn handshake-message [client]
  {:payload "HANDSHAKE"
   :meta {:type :register
          :service (:ident client)}})

(defn close-message [client]
  {:payload "CLOSE"
   :meta {:type :close
          :service (:ident client)}})

(defn send-channel [dealer]
  (let [ch (async/chan config/main-channel-buffer-size)
        stop-ch (async/chan)]
    (async/go-loop []
      (when (async/alt! stop-ch false :default :keep-going)
        (some->> ch
                 async/<!
                 (send-dealer-message! dealer))
        (recur)))
    {:stop-ch stop-ch
     :main-ch ch}))

(defn terminate-sender-channel! [{:keys [main-ch stop-ch]}]
  (async/close! stop-ch)
  (async/reduce (constantly nil) [] main-ch)
  (async/close! main-ch))

(defn terminate-dealer-socket! [dealer] (zmq/close dealer))

(defprotocol ZMQDealer
  (send-message! [this message-map]))

(defn start-heartbeat-loop [{{:keys [main-ch stop-ch]} :channels :as client} heartbeat-timing-ms]
  (async/go-loop []
    (when (async/alt! stop-ch false (async/timeout heartbeat-timing-ms) :keep-going)
      (send-message! client (heartbeat-msg client))
      (recur))))

(defn start-handshake-request [client]
  (send-dealer-message! (:dealer client) (handshake-message client)))

(defn send-close-request [client]
  (send-dealer-message! (:dealer client) (close-message client)))

(defrecord HiveClient [endpoint ident]
  component/Lifecycle
  (start [this]
    (let [dealer (new-dealer-socket! endpoint ident)
          stop-ch (async/chan)
          started-this (assoc this :dealer dealer :channels (send-channel dealer) :ident ident)]
      (start-handshake-request started-this)
      (start-heartbeat-loop started-this config/heartbeat-timing-ms)
      started-this))

  (stop [this]
    (terminate-sender-channel! (:channels this)) ;; Ensures that heartbeat delivery also is ended
    (send-close-request this)
    (terminate-dealer-socket! (:dealer this))
    (dissoc this :channels :dealer))

  ZMQDealer
  (send-message! [this message-map]
    (async/go
      (async/>! (-> this :channels :main-ch) (adapters/trace-payload message-map)))))

(defn new-hive-client! [endpoint ident]
  (component/start (->HiveClient endpoint ident)))

(defn terminate-client! [client]
  (component/stop client))
