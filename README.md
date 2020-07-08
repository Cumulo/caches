
Caches sulotion for Cumulo and Respo
----

> a caches library as a replacement for memoizations in Cumulo and Respo. It's mostly experimental.

### Usage

[![Clojars Project](https://img.shields.io/clojars/v/cumulo/caches.svg)](https://clojars.org/cumulo/caches)

```edn
[cumulo/caches "0.1.1-a4"]
```

```clojure
(defonce *caches (caches.core/new-caches))
(caches.core/show-summay! *caches)
```

Methods:

* `(new-caches)` returns an atom holding caches states
* `(show-summary! *caches)` list entries after formatted
* `(write-cache! *caches params value)` write to cache, `params` supposed to be a collection
* `(access-cache *caches params)` access and return value(or `nil`)
* `(new-loop! *caches)` loop and trigger actions
* `(perform-gc! *caches)` remove entries that are probably no longer useful
* `(reset-caches! *caches)` clear caches for debugging purposes

Storage structure:

```clojure
(atom {:loop 0, ; counter
       :caches {}, ; where caches are stored
       :gc {:cold-duration 400, ; wait for N loops before triggering GC
            :trigger-loop 100, ; trigger GC every N loops
            :elapse-loop 50}}) ; entries are considered unuseful after not used for N loops
```

### Workflow

https://github.com/mvc-works/calcit-nodejs-workflow

### License

MIT
