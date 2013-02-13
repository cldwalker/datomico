(ns datomic-simple.test-helper
  (:require [datomic.api :as d]
            [datomic-simple.db :as dsb]))

(def datomic-uri "datomic:mem://datomic-simple-test")

(defmacro with-db
  "Evaluates body with var bound to a connection"
  [& body]
  `(let [~'_ (d/create-database datomic-uri)]
     (binding [dsb/*uri* datomic-uri
               dsb/*connection* (d/connect datomic-uri)]
       (~'with-db-setup)
       (try
         (dsb/with-latest-database (do ~@body))
         (finally (d/delete-database datomic-uri))))))