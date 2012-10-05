(ns datomic-simple.core
  (:require [datomic.api :as d]
            clojure.string))

(def ^:dynamic *connection*)
(def ^:dynamic *db*)

; from https://gist.github.com/3150938
(defn wrap-datomic
  "A Ring middleware that provides a request-consistent database connection and
  value for the life of a request."
  [handler uri]
  (fn [request]
    (let [conn (d/connect uri)]
      (binding [*connection* conn
                *db*         (d/db conn)]
        (handler request)))))

(defn q [query & args]
  (apply d/q query *db* args))

(defn where [query & args]
  (->> (apply q query args)
    (mapv (fn [items]
      (mapv #(d/entity *db* %) items)))))

(defn entity->map [e]
  (merge (select-keys e (keys e))
         {:db/id (:db/id e)}))

(defn find-all [query & args]
  (->> (apply where query args) flatten (map entity->map))) 

(defn find-first [query & args]
  (first (apply find-all query args)))

(defn find-by [query-map]
  (let [query-string (clojure.string/join " "
                      (concat
                        ["[:find ?e :in $"]
                        (map #(str "?field" % " ?val" %) (range (count query-map)))
                        [":where"]
                        (map #(str "[?e ?field" % " ?val" % "]") (range (count query-map)))
                        ["]"]))]
    (apply find-all query-string (flatten (vec query-map)))))

(defn init [uri]
  (d/delete-database uri)
  (d/create-database uri))

(defn repl-init [uri]
  (def ^:dynamic *connection* (d/connect uri))
  (def ^:dynamic *db* (d/db *connection*)))

(defn transact [tx]
  (d/transact *connection* tx))

(defn transact! [tx]
  (prn "Transacting..." tx)
  @(transact tx))

(defn num-id [id]
  (Long. id))

(defn find-id [id]
  (let [entity (d/entity *db* (num-id id))]
    (if-not (empty? entity) (entity->map entity))))

(defn delete [& ids]
   (transact! (map #(vec [:db.fn/retractEntity (num-id %)]) ids)))

(defn bare-update [id attr]
  (transact! [(merge attr {:db/id (num-id id)})]))

(defn build-schema-attr [attr-name value-type & options]
  (let [cardinality (if (some #{:many} options)
                      :db.cardinality/many
                      :db.cardinality/one)
        fulltext    (not (boolean (some #{:nofulltext} options))) 
        history     (boolean (some #{:nohistory} options))
        index       (not (boolean (some #{:noindex} options)))]
    
    {:db/id           (d/tempid :db.part/db)
     :db/ident        attr-name
     :db/valueType    (keyword "db.type" (name value-type))
     :db/cardinality  cardinality
     :db/index        index
     :db/fulltext     fulltext
     :db/noHistory    history
     :db.install/_attribute :db.part/db}))
    
(defn build-schema [nsp attrs]
  (map #(apply build-schema-attr
               (keyword (name nsp) (name (first %))) (rest %))
       attrs))

(defn map-keys [oldmap kfn]
  (->> oldmap
    (map (fn [[key val]] [(kfn key) val])) flatten (apply hash-map)))

(defn localize-attr [attr]
  (map-keys attr #(keyword (name %))))

(defn all [query]
  (map localize-attr (find-all query)))

(defn local-find-id [id]
  (if-let [m (find-id id)] (localize-attr m)))

(defn namespace-keys [nsp attr]
  (map-keys attr #(keyword (name nsp) (name %))))

(defn local-find-by [nsp query-map]
  (map localize-attr (find-by (namespace-keys nsp query-map))))

(defn local-all-by [nsp field]
  (all (format "[:find ?e :where [?e %s/%s]]" nsp (name field))))

(defn local-find-first-by [nsp query-map]
  (first (local-find-by nsp query-map)))

(defn build-attr [nsp attr]
  (->> (namespace-keys nsp attr)
    (merge {:db/id (d/tempid :db.part/user)})))

(defn create [nsp attr]
  (transact! [(build-attr nsp attr)]))

(defn update [nsp id attr]
  (bare-update id (namespace-keys nsp attr)))

