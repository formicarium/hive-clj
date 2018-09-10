(ns hive-clj.models
  (:require [cheshire.core :as cheshire]
            [schema.core :as s])
  (:import java.time.LocalDateTime))

(s/defschema HttpTags {:method s/Str;; HTTP method of the request for the associated Span. E.g., "GET", "POST"
                       (s/optional-key :status_code) s/Int;; HTTP response status code for the associated Span. E.g., 200, 503, 404
                       :url s/Str;; URL of the request being handled in this segment of the trace, in standard URI format. E.g., "https://domain.net/path/to?resource=here"
                       })

(s/defschema DbTags {:instance s/Str;; Database instance name. E.g., In java, if the jdbc.url="jdbc:mysql://127.0.0.1:3306/customers", the instance name is "customers"
                     :statement s/Str;; A database statement for the given database type. E.g., for db.type="sql", "SELECT * FROM wuser_table"; for db.type="redis", "SET mykey 'WuValue'"
                     :type s/Str;; Database type. For any SQL database, "sql". For others, the lower-case database category, e.g. "cassandra", "hbase", or "redis"
                     :user s/Str;; Username for accessing database. E.g., "readonly_user" or "reporting_user"
                     })

(s/defschema Peer {:port s/Int;; Remote port. E.g., 80
                   :service s/Str;; Remote service name (for some unspecified definition of "service"). E.g., "elasticsearch", "a_custom_microservice", "memcache". Meaning should correspond with values set in service
                   })

(s/defschema SpanDirection (s/enum :consumer :producer))
(s/defschema SpanType (s/enum :http-in :http-out :kafka))
(s/defschema SpanKind (s/enum :start :end))

(s/defschema SpanTags {:http HttpTags
                       :direction SpanDirection
                       :peer Peer
                       :type SpanType
                       :kind SpanKind
                       })

(s/defschema SpanContext {:trace-id s/Str
                          :span-id s/Str
                          :parent-id s/Str})

(s/defschema SpanPayload {:body s/Any
                          :headers s/Any})

(s/defschema Span {:timestamp LocalDateTime
                   :tags SpanTags
                   :payload SpanPayload
                   :context SpanContext})

(s/defschema HiveMessage {:meta {:type s/Keyword
                                 :service s/Keyword}
                          :payload Span
                          :identity s/Str})
