# mpg - more modern postgres to the gallon

Eases interoperability between clojure/java datatypes and postgres
datatypes. No more boilerplate!

Handles the following:

- DATE <-> java.time.LocalDate
- TIMESTAMP <-> java.time.Instant
- TIMESTAMPTZ <-> java.time.ZonedDateTime
- JSON/JSONB <-> clojure map/vector
- ARRAY <-> clojure vector
- HSTORE <-> clojure map

Please note that HSTORE is not as rich as JSON, everything in the map becomes a string

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/mpg.svg)](https://clojars.org/mpg)

Just require the `mpg.core` namespace and call `patch`

```clojure
(ns whatever.db
    (require [clojure.java.jdbc :as j]
             [mpg.core :as mpg]]))
(mpg/patch) ;; take the default settings
(mpg/patch {:default-map :hstore}) ;; custom settings are merged with the defaults
;; valid settings:
    :data        - boolean, default true. auto-map maps and vectors?
    :datetime    - boolean, default true. auto-map java.time.{LocalDate, ZonedDateTime} ?
    :default-map - keyword. one of :json, :hstore. Default :json
```

## Limitations

The current clojure.java.jdbc interface imposes some limitations on us.

1. You only get the autoconversion when using clojure.java.jdbc or something built on it
2. When using unbound statements, we cannot save a vector as an array type (we therefore use json)
3. When using unbound statements, you must choose between storing maps as json or hstore (default: json)
4. All applications that have written to the database are assumed to have correctly saved timestamps in UTC. If you only use this library, you won't have to worry about that. Most applications can be configured with the TZ environment variable

## Contributing

Contributions and improvements welcome, just open an issue! :)

If this library should do something, and it doesn't currently do it, please fork
and open a pull request. All reasonable contributions will be accepted.

## Acknowledgements

This library was originally extracted from luminus boilerplate which
hails from code floating around the internet
generally. [James Laver](https://github.com/jjl) then contributed a
new API and java.time support.

## License

Copyright © 2016 Shane Kilkelly

Distributed under the MIT license.
