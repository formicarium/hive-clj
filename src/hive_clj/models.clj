(ns hive-clj.models
  (:require [cheshire.core :as cheshire]
            [schema.core :as s])
  (:import java.time.LocalDateTime))

(s/defschema HttpTags {:method s/Str;; HTTP method of the request for the associated Span. E.g., "GET", "POST"
                       :status_code s/Int;; HTTP response status code for the associated Span. E.g., 200, 503, 404
                       :url s/Str;; URL of the request being handled in this segment of the trace, in standard URI format. E.g., "https://domain.net/path/to?resource=here"
                       })

(s/defschema DbTags {:instance s/Str;; Database instance name. E.g., In java, if the jdbc.url="jdbc:mysql://127.0.0.1:3306/customers", the instance name is "customers"
                     :statement s/Str;; A database statement for the given database type. E.g., for db.type="sql", "SELECT * FROM wuser_table"; for db.type="redis", "SET mykey 'WuValue'"
                     :type s/Str;; Database type. For any SQL database, "sql". For others, the lower-case database category, e.g. "cassandra", "hbase", or "redis"
                     :user s/Str;; Username for accessing database. E.g., "readonly_user" or "reporting_user"
                     })

(s/defschema PeerTags {:peer.port s/Int;; Remote port. E.g., 80
                       :peer.service s/Str;; Remote service name (for some unspecified definition of "service"). E.g., "elasticsearch", "a_custom_microservice", "memcache". Meaning should correspond with values set in service
                       })
(s/defschema MessageBusTags {:message_bus.destination s/Str;; An address at which messages can be exchanged. E.g. A Kafka record has an associated "topic name" that can be extracted by the instrumented producer or consumer and stored using this tag.
                             })

(s/defschema SpanTags {:http HttpTags
                       :direction s/Str;; Either "client" or "server" for the appropriate roles in an RPC, and "producer" or "consumer" for the appropriate roles in a messaging scenario
                       :type s/Str;; A stable identifier for some notable moment in the lifetime of a Span. For instance, a mutex lock acquisition or release or the sorts of lifetime events in a browser page load described in the Performance.timing specification. E.g., from Zipkin, "cs", "sr", "ss", or "cr". Or, more generally, "initialized" or "timed out". For errors, "error"
                       })

(s/defschema SpanContext {:trace-id s/Str
                          :span-id s/Str
                          :parent-id s/Str})

(s/defschema Span {:start LocalDateTime
                   :end LocalDateTime
                   :tags SpanTags
                   :payload s/Str
                   :context SpanContext})

(s/defschema HiveMessage {:meta {:type s/Keyword
                                 :service s/Keyword}
                          :payload Span
                          :identity s/Str})
