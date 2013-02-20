(ns datomico.action
  (:require [datomico.db :as db])
  (:require clojure.string))
;;; basic CRUD fns, useful for any datomic app i.e. no namespace assumptions

(defn raw-where
  "Queries with datomic.api/q and converts results to entities."
  [query & args]
  (->> (apply db/q query args)
    (mapv (fn [items]
      (mapv db/entity items)))))

(defn entity->map
  "Converts an entity into a map with :db/id added."
  [e]
  (merge (select-keys e (keys e))
         {:db/id (:db/id e)}))

(defn id->map
  "Fetches map for id"
  [id]
  (entity->map (db/entity id)))

(defn where
  "Queries with datomic.api/q and converts results into a vector of maps."
  [query & args]
  (->> (apply raw-where query args) flatten (map entity->map))) 

(defn find-first
  "Queries with datomic.api/q and returns first result as a map. Returns nil if nothing found."
  [query & args]
  (first (apply where query args)))

(defn all
  "Returns all entities for a given namespace."
  [nsp]
  (where '[:find ?e
                  :in $ ?nsp
                  :where [?e ?attr]
                         [?attr :db/ident ?name]
                         [(#(= (namespace %1) (name %2)) ?name ?nsp)]]
                nsp))

;; TODO: build a data structure instead of a string
(defn find-all
  "Queries with given map of attribute names to values and returns a vector of maps."
  [query-map]
  (let [query-string (clojure.string/join " "
                      (concat
                        ["[:find ?e :in $"]
                        (map #(str "?field" % " ?val" %) (range (count query-map)))
                        [":where"]
                        (map #(str "[?e ?field" % " ?val" % "]") (range (count query-map)))
                        ["]"]))]
    (apply where query-string (flatten (vec query-map)))))

(defn- num-id [id]
  (Long. id))

(defn find-id
  "If entity is found for id, return it as a map. Otherwise return nil."
  [id]
  (let [ent (db/entity (num-id id))]
    (if-not (empty? ent) (entity->map ent))))

(defn delete
  "Deletes given ids."
  [& ids]
  (db/transact! (map #(vec [:db.fn/retractEntity (num-id %)]) ids)))

;; could make value optional by just fetching it
(defn delete-value-tx [id attr value]
  "Generates transaction data to delete value from entity."
  [:db/retract id attr value])

(defn update-tx
  "Updates given id with map of attributes."
  [id attr]
  (merge attr {:db/id (num-id id)}))
