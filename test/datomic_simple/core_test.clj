(ns datomic-simple.core-test
  (:require [datomic.api :as d]
            [datomic-simple.core :refer :all]
            [clojure.test :refer :all]
            [datomic-simple.db :as dsb]))

(def datomic-uri "datomic:mem://datomic-simple-test")

(defmacro with-db
  "Evaluates body with var bound to a connection"
  [& body]
  `(let [~'_ (d/create-database datomic-uri)]
     (binding [dsb/*uri* datomic-uri
               dsb/*connection* (d/connect datomic-uri)]
       (try
         (dsb/with-latest-database (do ~@body))
         (finally (d/delete-database datomic-uri))))))

(deftest build-schema-test
  (testing "a property defaults to correct values"
    (is (= {:db/ident :animal/sound
            :db/doc nil
            :db/valueType :db.type/string
            :db/isComponent false
            :db/cardinality :db.cardinality/one
            :db/index false
            :db/fulltext true
            :db/noHistory false
            :db/unique nil}
           (-> (build-schema :animal [[:sound :string]])
               first
               (dissoc :db.install/_attribute :db/id)))))
  (testing ":fulltext defaults to false if not a string"
    (is (not
           (->
            (build-schema :animal [[:behaviors :ref]])
            first
            :db/fulltext))))
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
  (testing ":index enables :db/index"
    (is (->
         (build-schema :account [[:password-hash :string :index]])
         first
         :db/index)))
  (testing ":nofulltext disables :db/fulltext"
    (is (not
         (->
          (build-schema :account [[:password-hash :string :nofulltext]])
          first
          :db/fulltext))))
  (testing ":unique adds a :db.unique/value"
    (is (= :db.unique/value
         (->
          (build-schema :user [[:name :string :unique]])
          first
          :db/unique))))
  (testing "string at end of attr sets :db/doc"
    (is (= "XXXX"
         (->
          (build-schema :account [[:password-hash :string "XXXX"]])
          first
          :db/doc))))
  (testing ":component sets correct db attributes"
    (is (= {:db/isComponent true :db/valueType :db.type/ref}
           (->
            (build-schema :comment [[:body :component]])
            first
            (select-keys [:db/isComponent :db/valueType])))))
  (testing "invalid :db/type raises a more helpful error than datomic's default"
    (with-db
      (is (thrown-with-msg? java.util.concurrent.ExecutionException #":db.type/doh is not.*valid"
            @(d/transact dsb/*connection* (build-schema :user [[:name :doh]])))))))