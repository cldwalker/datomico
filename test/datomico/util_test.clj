(ns datomico.util-test
  (:require [datomico.util :as util]
            [clojure.test :refer :all]))

(deftest localize-attr-test
  (let [namespaced-map {:ab/a 2 :ab/b 3 :ab/c 5}
        ns-map2 {:hdys</b45 "rhd" :hdgsyV "7d99"}]
    (is (= {:a 2 :b 3 :c 5}
           (util/localize-attr namespaced-map)))
    (is (= {:b45 "rhd" :hdgsyV "7d99"}
           (util/localize-attr ns-map2)))))

(deftest namespace-keys-test
  (let [localized-map {:foo :bar :baz :quux}]
    (is (= {:boo/foo :bar :boo/baz :quux}
           (util/namespace-keys :boo localized-map)))
    (is (= {:boo/foo :bar :boo/baz :quux}
           (util/namespace-keys "boo" localized-map)))))
