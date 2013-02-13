(ns datomic-simple.core-test
  (:require [datomic.api :as d]
            [datomic-simple.core :refer :all]
            [clojure.test :refer :all]
            [datomic-simple.test-helper :refer [with-db]]
            [datomic-simple.db :as dsb]))

(defn with-db-setup [])

(defn build-and-transact-schema [nsp attrs]
  (let [data (build-schema nsp attrs)]
    (with-db
      @(d/transact dsb/*connection* data))
    data))

(deftest build-schema-test
  (testing "a property defaults to correct values"
    (is (= {:db/ident :animal/sound
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one
            :db/index false
            :db/fulltext true
            :db/noHistory false}
           (-> (build-and-transact-schema :animal [[:sound :string]])
               first
               (dissoc :db.install/_attribute :db/id)))))
  (testing ":fulltext defaults to false if not a string"
    (is (not
           (->
            (build-and-transact-schema :animal [[:behaviors :ref]])
            first
            :db/fulltext))))
  (testing ":many sets cardinality of many"
    (is (= :db.cardinality/many
           (->
            (build-and-transact-schema :animal [[:behaviors :ref :many]])
            first
            :db/cardinality))))
  (testing ":nohistory enables :db/noHistory"
    (is (->
         (build-and-transact-schema :account [[:password-hash :string :nohistory]])
         first
         :db/noHistory)))
  (testing ":index enables :db/index"
    (is (->
         (build-and-transact-schema :account [[:password-hash :string :index]])
         first
         :db/index)))
  (testing ":nofulltext disables :db/fulltext"
    (is (not
         (->
          (build-and-transact-schema :account [[:password-hash :string :nofulltext]])
          first
          :db/fulltext))))
  (testing ":unique adds a :db.unique/value"
    (is (= :db.unique/value
         (->
          (build-and-transact-schema :user [[:name :string :unique]])
          first
          :db/unique))))
  (testing "string at end of attr sets :db/doc"
    (is (= "XXXX"
         (->
          (build-and-transact-schema :account [[:password-hash :string "XXXX"]])
          first
          :db/doc))))
  (testing ":component sets correct component attributes"
    (is (= {:db/isComponent true :db/valueType :db.type/ref}
           (->
            (build-and-transact-schema :comment [[:body :component]])
            first
            (select-keys [:db/isComponent :db/valueType])))))
  (testing ":ref and :component sets correct component attributes"
    (is (= {:db/isComponent true :db/valueType :db.type/ref}
           (->
            (build-and-transact-schema :comment [[:body :ref :component]])
            first
            (select-keys [:db/isComponent :db/valueType])))))
  (testing "invalid :db/type raises a more helpful error than datomic's default"
    (with-db
      (is (thrown-with-msg? java.util.concurrent.ExecutionException #":db.type/doh is not.*valid"
            @(d/transact dsb/*connection* (build-schema :user [[:name :doh]])))))))