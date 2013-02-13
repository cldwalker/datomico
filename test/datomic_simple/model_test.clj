(ns datomic-simple.model-test
  (:require [clojure.test :refer :all :exclude [testing]]
            [datomic-simple.test-helper :refer [always-with-latest-db]]
            [datomic-simple.db :refer [with-latest-database] :as dsb]
            datomic-simple.core
            [datomic-simple.model :refer :all]))

(defn load-schemas []
  (let [schema (datomic-simple.core/build-schema :item [[:name :string] [:url :uri]])]
    (dsb/load-schemas schema)))

(defmacro testing [str & body]
  `(clojure.test/testing ~str
     (always-with-latest-db
       (load-schemas)
       ~@body)))

(deftest find-id-test
  (testing "finds by id and returns map"
    (let [ent (create :item {:name "dude"})]
      (is (= {:id (ent :id) :name "dude"} (find-id (:id ent))))))
  (testing "doesn't find id and returns nil"
    (let [ent (create :item {:name "dude"})]
      (is (nil? (find-id (inc (:id ent))))))))