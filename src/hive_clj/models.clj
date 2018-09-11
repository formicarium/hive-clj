(ns hive-clj.models
  (:require [cheshire.core :as cheshire]
            [schema.core :as s])
  (:import java.time.LocalDateTime))

(s/defschema HttpTags {:method s/Str
                       (s/optional-key :status_code) s/Int
                       :url s/Str})

(s/defschema DbTags {:instance s/Str
                     :statement s/Str
                     :type s/Str
                     :user s/Str})

(s/defschema Peer {:port s/Int
                   :service s/Str})

(s/defschema SpanDirection (s/enum :consumer :producer))
(s/defschema SpanType (s/enum :http-in :http-out :kafka))
(s/defschema SpanKind (s/enum :start :end))

(s/defschema SpanTags {:http HttpTags
                       :direction SpanDirection
                       :peer Peer
                       :type SpanType
                       :kind SpanKind})

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
