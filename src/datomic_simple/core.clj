(ns datomic-simple.core
  (:use datomic-simple.db)
  (:require [datomic.api :as d]
            [datomic-simple.actions :as actions]))

(defmacro ^:private add-noir-middleware [uri]
  `(noir.server/add-middleware wrap-datomic ~uri))

(defn rand-connection []
  (str "datomic:mem://" (java.util.UUID/randomUUID)))

(defn start [{:keys [uri schemas seed-data repl]}]
  (let [uri (or uri (rand-connection))]
    (set-uri uri)
    (d/create-database uri)
    (when (seq schemas)
      (load-schemas uri schemas))
    (when (seq seed-data)
      (load-seed-data uri seed-data))
    (when (some #{"noir.server"} (map str (all-ns)))
      (add-noir-middleware uri))
    (when repl
      (repl-init uri))))

(defn build-schema-attr [attr-name value-type & options]
  (let [cardinality (if (some #{:many} options)
                      :db.cardinality/many
                      :db.cardinality/one)
        fulltext    (if-not (= value-type :string) false 
                      (not (boolean (some #{:nofulltext} options))))
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

(defn namespace-keys [nsp attr]
  (map-keys attr #(keyword (name nsp) (name %))))

(defn build-seed-data [nsp attrs]
  (map (partial namespace-keys nsp) attrs))

(defn localize-attr [attr]
  (map-keys attr #(keyword (name %))))

(defn all [query]
  (map localize-attr (actions/find-all query)))

(defn local-find-id [id]
  (if-let [m (actions/find-id id)] (localize-attr m)))

(defn local-find-by [nsp query-map]
  (map localize-attr (actions/find-by (namespace-keys nsp query-map))))

(defn expand-ref [m]
  (if (empty? m) nil (localize-attr (actions/entity->map m))))

(defn delete-by [nsp query-map]
  (let [results (local-find-by nsp query-map)]
    (when (seq results)
      (apply actions/delete (map :id results)))))

(defn local-all-by [nsp field]
  (all (format "[:find ?e :where [?e %s/%s]]" nsp (name field))))

(defn local-find-first-by [nsp query-map]
  (first (local-find-by nsp query-map)))

(defn build-attr [nsp attr]
  (add-new-id (namespace-keys nsp attr)))

(defn create [nsp attr]
  (let [nsp-attr (build-attr nsp attr)
        tx-result (transact! [nsp-attr])
        new-id (resolve-tempid (:tempids tx-result) (:db/id nsp-attr))]
    (merge attr {:id new-id})))

(defn update [nsp id attr]
  (actions/bare-update id (namespace-keys nsp attr)))

(defmacro create-model-fn
  "Creates a local function that wraps a datomic-simple fn with a namespace arg."
  [fn-name nsp]
  `(do
    (def ~(symbol (name fn-name))
      (partial ~(deref (resolve (symbol "datomic-simple.core" (name fn-name)))) ~nsp))))

(defmacro create-model-fns-for
  "Creates model fns that scope to the given model namespace."
  [nsp]
  `(do
    (create-model-fn :create ~nsp)
    (create-model-fn :local-all-by ~nsp)
    (create-model-fn :local-find-first-by ~nsp)
    (create-model-fn :local-find-by ~nsp)
    (create-model-fn :delete-by ~nsp)
    (create-model-fn :update ~nsp)))
