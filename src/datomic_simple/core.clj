(ns datomic-simple.core
  (:require [datomic.api :as api]
            [datomic-simple.model :as dm]
            datomic-simple.action
            [datomic-simple.db :as db]
            [datomic-simple.util :as util]))

(defn- add-noir-middleware [uri]
  (@(resolve (symbol "noir.server" "add-middleware")) db/wrap-datomic uri))

(defn- rand-connection []
  (str "datomic:mem://" (java.util.UUID/randomUUID)))

(defn start
  "Start datomic by creating a connection, creating a database and optionally loading schema and
  seed data.  When calling this from a noir app, appopriate middleware is added so the latest
  database value and connection are made available.
  Options:

  :uri       Specify a uri string. Defaults to a randomly generated value.
  :schemas   A vector of schemas, each schema representing a model. A schema is a vector of maps,
             each requiring 3 attributes. They can be simply defined using build-schema. Default is nil.
  :seed-data A vector of vectors, each subvector representing seed data for a model. This is loaded
             after schemas. Default is nil.
  :repl      A boolean when enabled that allows all datomic fns in the repl to work. Default is false.
  "
  [{:keys [uri schemas seed-data repl]}]
  (let [uri (or uri (rand-connection))]
    (db/set-uri uri)
    (api/create-database uri)
    (when (seq schemas)
      (db/load-schemas schemas))
    (when (seq seed-data)
      (db/load-seed-data uri seed-data))
    (when (some #{"noir.server"} (map str (all-ns)))
      (add-noir-middleware uri))
    (when repl
      (db/repl-init uri))))


;;allowable types in datomic
;;http://docs.datomic.com/schema.html

;:db.type/keyword - Value type for keywords. Keywords are used as names, and are interned for efficiency. Keywords map to the native interned-name type in languages that support them.
;:db.type/string - Value type for strings.
;:db.type/boolean - Boolean value type.
;:db.type/long - Fixed integer value type. Same semantics as a Java long: 64 bits wide, two's complement binary representation.
;:db.type/bigint - Value type for arbitrary precision integers. Maps to java.math.BigInteger on Java platforms.
;:db.type/float - Floating point value type. Same semantics as a Java float: single-precision 32-bit IEEE 754 floating point.
;:db.type/double - Floating point value type. Same semantics as a Java double: double-precision 64-bit IEEE 754 floating point.
;:db.type/bigdec - Value type for arbitrary precision floating point numbers. Maps to java.math.BigDecimal on Java platforms.
;:db.type/ref - Value type for references. All references from one entity to another are through attributes with this value type.
;:db.type/instant - Value type for instants in time. Stored internally as a number of milliseconds since midnight, January 1, 1970 UTC. Maps to java.util.Date on Java platforms.
;:db.type/uuid - Value type for UUIDs. Maps to java.util.UUID on Java platforms.
;:db.type/uri - Value type for URIs. Maps to java.net.URI on Java platforms.
;:db.type/bytes - Value type for small binary data. Maps to byte array on Java platforms.

(def permitted-types (sorted-set :db.type/bytes :db.type/uri :db.type/uuid :db.type/instant 
                                 :db.type/keyword :db.type/string :db.type/boolean 
                                 :db.type/long :db.type/bigint :db.type/float :db.type/double 
                                 :db.type/bigdec :db.type/ref))
(defn- type-permitted? 
  "test if the tpye is allowd in datomic, if it's not then throw an error, if it is then return the input"
  [type]
  (if (contains? permitted-types type)
      type
      (throw (Exception. (str "type: " type " is not allowed as a datomic schema type." \newline " Allowed types: " permitted-types)))))

(defn- build-schema-attr [attr-name value-type & options]
  (let [
       
       ;;assume that the last thing in options is a doc string, only if it is a string
       documentation (if (string? (last options)) (last options) nil)
       cardinality   (if (some #{:many} options) :db.cardinality/many :db.cardinality/one)
       fulltext?    (if-not (= value-type :string) false (not (boolean (some #{:nofulltext} options))))
       history?     (boolean (some #{:nohistory} options))
       index?       (boolean (some #{:index} options)) ;index is not turned on by default, since it signifcantly slows down transactional write throughput
       unique?      (if (some #{:unique} options) :db.unique/value nil)
       component?   (boolean (or (= value-type :component)(some #{:component} options)))
       type         (if component? :db.type/ref (type-permitted? (keyword "db.type" (name value-type))))]
    
    {:db/id           (api/tempid :db.part/db)
     :db/ident        attr-name
     :db/doc          documentation
     :db/valueType    type
     :db/isComponent  component?
     :db/cardinality  cardinality
     :db/index        index?
     :db/fulltext     fulltext?
     :db/noHistory    history?
     :db/unique       unique?
     :db.install/_attribute :db.part/db}))
    
(defn build-schema
  "Given a keyword namespace and a vector of vectors, creates a schema as described at
  http://docs.datomic.com/schema.html.  Each subvector represents a field and only requires a name
  and type. By default, all fields are indexed, have history and are assumed to have a cardinality
  of one. To override these defaults, a subvector can contain these additional elements:

  :many       Indicates a cardinality of many.
  :nofulltext Disables fulltext. If a field is of type string, fulltext is enabled by default.
  :nohistory  Disables history.
  :index      Enables index.

  Example:
  (build-schema :user
  [[:name :string :nofulltext]
  [:email :string :unique :index]
  [:limb :component]])
  
  (build-schema :arm
  [[:left :boolean]
  [:right :boolean]
  [:hand :component]])
  "
  [nsp attrs]
  (map #(apply build-schema-attr
               (keyword (name nsp) (name (first %))) (rest %))
       attrs))

(defn build-seed-data
  "Given a keyword namespace and a vector of data, prepares seed data for a transaction."
  [nsp attrs]
  (map (partial util/namespace-keys nsp) attrs))

(def find-id datomic-simple.model/find-id)
(def expand-ref datomic-simple.model/expand-ref)
(def delete datomic-simple.action/delete)

; TODO: allow user to pass in which fns they want to create
(defmacro create-model-fns
  "Creates model fns that are scoped to the given model (keyword namespace). Creates the following
  fns: create, update, delete-all, find-all and find-first."
  [nsp]
  `(do
    (dm/create-model-fn :create ~nsp)
    (dm/create-model-fn :find-first ~nsp)
    (dm/create-model-fn :find-all ~nsp)
    (dm/create-model-fn :delete-all ~nsp)
    (dm/create-model-fn :update ~nsp)))
