(ns datomic-simple.test-helper
  (:require [datomic.api :as d]
            [datomic-simple.db :as dsb]))

(def datomic-uri "datomic:mem://datomic-simple-test")

(defmacro with-db
  "Wraps a body in a datomic connection, db and uri. If any updates are made
to *db*, you must rebind *db* to the latest database."
  [& body]
  `(let [~'_ (d/create-database datomic-uri)]
     (binding [dsb/*uri* datomic-uri
               dsb/*connection* (d/connect datomic-uri)]
       (try
         (dsb/with-latest-database (do ~@body))
         (finally (d/delete-database datomic-uri))))))

(defmacro always-with-latest-db
  "Wraps body in a datomic connection and uri. It always gets the latest db for any query.
This is purely a testing convenience. DO NOT use in production."
  [& body]
  `(let [~'_ (d/create-database datomic-uri)]
     (binding [dsb/*uri* datomic-uri
               dsb/*connection* (d/connect datomic-uri)]
       (dsb/repl-init dsb/*uri*)
       (try
         (do ~@body)
         (finally (d/delete-database datomic-uri))))))