(ns datomic-simple.util)

(defn map-keys [oldmap kfn]
  (->> oldmap
    (map (fn [[key val]] [(kfn key) val])) flatten (apply hash-map)))

(defn namespace-keys [nsp attr]
  (map-keys attr #(keyword (name nsp) (name %))))
