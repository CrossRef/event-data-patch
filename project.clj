(defproject event-data-patch "0.1.0"
  :description "Event Data Patch"
  :url "http://eventdata.crossref.org/"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.cache "0.6.5"]
                 [org.clojure/core.async "0.2.395"]
                 [event-data-common "0.1.20"]
                 [prismatic/schema "1.1.3"]
                 [clj-http "2.3.0"]
                 [http-kit.fake "0.2.1"]
                 [org.clojure/data.json "0.2.6"]
                 [crossref-util "0.1.10"]
                 [robert/bruce "0.8.0"]
                 [yogthos/config "0.8"]
                 [clj-time "0.12.2"]
                 [com.amazonaws/aws-java-sdk "1.11.61"]
                 [com.github.crawler-commons/crawler-commons "0.7"]
                 [com.novemberain/monger "3.1.0"]
                 [cheshire "5.7.0"]
                 
                 ; Required for AWS, but not fetched.
                 [org.apache.httpcomponents/httpclient "4.5.3"] ; Reqired for TLS SNI.
                 [org.apache.commons/commons-io "1.3.2"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.apache.logging.log4j/log4j-core "2.6.2"]
                 [org.slf4j/slf4j-simple "1.7.21"]]
  :main ^:skip-aot event-data-patch.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
