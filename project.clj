(defproject datomic-simple "0.1.0"
  :description "Use datomic with less cognitive overhead"
  :url "http://github.com/cldwalker/datomic-simple"
  :license {:name "The MIT License"
            :url "https://en.wikipedia.org/wiki/MIT_License"}
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :profiles {:dev {:dependencies [[com.datomic/datomic-free "0.8.3789"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0-RC1"]]}}
  :aliases {"all" ["with-profile" "dev:dev,1.5"]})
