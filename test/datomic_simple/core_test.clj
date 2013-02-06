(ns datomic-simple.core-test
  (:use clojure.test
        datomic-simple.core)
  (:require [datomic.api :as d]
            [datomic-simple.db :as dsb]))

(def datomic-uri "datomic:mem://datomic-simple-test")

(defmacro with-latest-db [& body]
  `(binding [datomic-simple.db/*db* (d/db datomic-simple.db/*connection*)]
     ~@body))

(defmacro with-db
  "Evaluates body with var bound to a connection"
  [& body]
  `(let [~'_ (d/create-database datomic-uri)]
     (binding [dsb/*uri* datomic-uri
               dsb/*connection* (d/connect datomic-uri)]
       (dsb/load-schemas thing/schema)
       (try
         (with-latest-db (do ~@body)) 
         (finally (d/delete-database datomic-uri))))))

(deftest build-schema-test
  (testing "a property defaults to correct values"
    (is (= {:db/ident :animal/sound
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one
            :db/index true
            :db/fulltext true
            :db/noHistory false}
           (-> (build-schema :animal [[:sound :string]])
               first
               (dissoc :db.install/_attribute :db/id)))))
  (testing ":many sets cardinality of many"
    (is (= :db.cardinality/many
           (->
            (build-schema :animal [[:behaviors :ref :many]])
            first
            :db/cardinality))))
  (testing ":nohistory enables :db/noHistory"
    (is (->
         (build-schema :account [[:password-hash :string :nohistory]])
         first
         :db/noHistory)))
  (testing ":noindex disables :db/index"
    (is (not
         (->
          (build-schema :account [[:password-hash :string :noindex]])
          first
          :db/index))))
  (testing ":nofulltext disables :db/fulltext"
    (is (not
         (->
          (build-schema :account [[:password-hash :string :nofulltext]])
          first
          :db/fulltext)))))