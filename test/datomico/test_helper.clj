(ns datomico.test-helper
  (:require [datomic.api :as d]
            [datomico.db :as db]))

(def datomic-uri "datomic:mem://datomico-test")

(defmacro with-db
  "Wraps a body in a datomic connection, db and uri. If any updates are made
to *db*, you must rebind *db* to the latest database."
  [& body]
  `(let [~'_ (d/create-database datomic-uri)]
     (binding [db/*uri* datomic-uri
               db/*connection* (d/connect datomic-uri)]
       (try
         (db/with-latest-database (do ~@body))
         (finally (d/delete-database datomic-uri))))))

(defmacro always-with-latest-db
  "Wraps body in a datomic connection and uri. It always gets the latest db for any query.
This is purely a testing convenience. DO NOT use in production."
  [& body]
  `(let [~'_ (d/create-database datomic-uri)]
     (binding [db/*uri* datomic-uri
               db/*connection* (d/connect datomic-uri)]
       (db/init-dynamic-variables db/*uri*)
       (try
         (do ~@body)
         (finally (d/delete-database datomic-uri))))))
