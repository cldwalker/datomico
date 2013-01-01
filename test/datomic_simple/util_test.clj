(ns datomic-simple.util-test
  (:require [datomic-simple.util :as util])
  (:use clojure.test))

(deftest tests-for-utils
  (testing "localize-attr"
    (let [namespaced-map {:ab/a 2 :ab/b 3 :ab/c 5}
          ns-map2 {:hdys</b45 "rhd" :hdgsyV "7d99"}]
      (is (= {:a 2 :b 3 :c 5}
             (util/localize-attr namespaced-map)))
      (is (not (= {:b45 "rhd" :hu74> "7d99"}
                  (util/localize-attr ns-map2))))
      (is (= {:b45 "rhd" :hdgsyV "7d99"}
             (util/localize-attr ns-map2)))))
  (testing "namespace-keys"
    (let [localized-map {:foo :bar :baz :quux}]
      (is (= {:boo/foo :bar :boo/baz :quux}
             (util/namespace-keys :boo localized-map)))
      (is (= {:boo/foo :bar :boo/baz :quux}
             (util/namespace-keys "boo" localized-map))))))
