(ns datomic-simple.model-test
  (:require [clojure.test :refer :all :exclude [testing]]
            [datomic-simple.test-helper :refer [with-db]]
            [datomic-simple.db :as dsb]
            [datomic-simple.model :refer :all]))

(defn with-db-setup []
  (let [schema (datomic-simple.core/build-schema :item [[:name :string] [:url :uri]])]
    (dsb/load-schemas schema)))

(defmacro testing [str & body]
  `(clojure.test/testing ~str
                         (with-db ~@body)))

(deftest find-id-test
  (testing "todo"
    (is true)))