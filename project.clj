(defproject formicarium/hive-clj "0.1.6-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :repositories [["sonatype" {:url "https://oss.sonatype.org/content/repositories/snapshots"
                              :update :always}]]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/java.data "0.1.1"]
                 [cheshire "5.8.0"]
                 [prismatic/schema "1.1.9"]
                 [org.zeromq/jeromq "0.4.3"]
                 [org.zeromq/cljzmq "0.1.4" :exclusions [org.zeromq/jzmq]]
                 [com.stuartsierra/component "0.3.2"]]
  :profiles {:dev     {:aliases      {"run-dev" ["trampoline" "run" "-m" "hive.core/start!"]}
                       :plugins      [[lein-midje "3.2.1"]]
                       :dependencies [[io.pedestal/pedestal.service-tools "0.5.3"]
                                      [nubank/matcher-combinators "0.2.8"]
                                      [midje "1.9.1"]]}})
