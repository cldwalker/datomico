## Description

Use datomic with less cognitive overhead. Should work with datomic >= 0.8.3789.

[![Build Status](https://travis-ci.org/cldwalker/datomico.png?branch=master)](https://travis-ci.org/cldwalker/datomico)

## Usage

Defining a model and starting datomic should be easy:

```clojure
(ns models.user)
(require '[datomico.core :as dc])

; Define a model's database key once and don't think of it again while
; interacting with your model
(def model-namespace :user)

; Build schemas easily without needing to think of partitions and a number of
; internal schema attributes. Basically you don't have to pour over
; http://docs.datomic.com/schema.html
(def schema (dc/build-schema model-namespace
  [[:username :string]
  [:password :string]]))

; Start datomic and initialize schemas without needing to think of
; database values and connections
(ns server)
(require '[datomico.core :as dc])
(dc/start {:uri "datomic:mem://my-app"
           :schemas [models.user/schema]})

; Starting in a repl is just as easy, a uri will be auto-generated
(ns user)
(require '[datomico.core :as dc])
(dc/start {:dynamic-vars true
           :schemas [models.user/schema]})
```

Creation, updating, deleting and querying should be easy for models:

```clojure
(ns models.user)
; creates several helper functions that scope your database interaction to the model.
(dc/create-model-fns model-namespace)

(create {:username "dodo" :password "bird"})
; => {:username "dodo" :password "bird" :id 1024053}
(find-all {:username "dodo"})
; => ({:username "dodo" :password "bird" :id 1024053})
(find-first {:password "bird"})
; => {:username "dodo" :password "bird" :id 1024053}

(update 1024053 {:username "big"})
(dc/delete 1024053)
```

## TODO
* More tests!

## Bugs/Issues

Please report them [on github](http://github.com/cldwalker/datomico/issues).

## License

See LICENSE.TXT

## Credits
* @relevance for fridays!
* @bobby for much of the initial abstraction - https://gist.github.com/3150938
* @boxxxie - for schema improvements
* @ravster - for tests

## Links
* [Sample blog using datomico](https://github.com/cldwalker/datomic-noir-blog) - Needs to be
  updated. Last known working commit - 50da3787e11608a98ed54220a371c7035ccdfa12.
* [Sample pastebin using datomico](https://github.com/cldwalker/datomic-refheap) - Needs to be
  updated. Last known working commit - 50da3787e11608a98ed54220a371c7035ccdfa12.
