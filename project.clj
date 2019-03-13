(defproject formicarium/hive-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :repositories [["sonatype" {:url "https://oss.sonatype.org/content/repositories/snapshots"
                              :update :always}]]
  :java-source-paths ["src/java/"]
  :source-paths ["src/clojure"]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :test-paths ["test/" "integration/"]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [cheshire "5.8.0"]
                 [io.opentracing/opentracing-api "0.31.0"]
                 [org.zeromq/jeromq "0.4.3"]
                 [org.zeromq/cljzmq "0.1.5-SNAPSHOT" :exclusions [org.zeromq/jzmq]]
                 [com.fasterxml.jackson.core/jackson-annotations "2.9.7"]
                 [com.fasterxml.jackson.core/jackson-core "2.9.7"]
                 [com.fasterxml.jackson.core/jackson-databind "2.9.7"]
                 [com.stuartsierra/component "0.3.2"]
                 [io.pedestal/pedestal.log "0.5.5"]])
