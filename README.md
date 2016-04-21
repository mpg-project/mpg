# mpg - more modern postgres to the gallon

Eases interoperability between clojure/java datatypes and postgres
datatypes. No more boilerplate!

Handles the following:

- DATE <-> java.time.LocalDate
- TIMESTAMP <-> java.time.Instant
- TIMESTAMPTZ <-> java.time.ZonedDateTime
- JSON/JSONB <-> clojure map/vector
- ARRAY <-> clojure vector

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/mpg.svg)](https://clojars.org/mpg)

Just require the `mpg.core` namespace and call `patch`

```clojure
(ns whatever.db
    (require [clojure.java.jdbc :as j]
             [mpg.core :as mpg]]))
      "
(mpg/patch) ;; take the default settings
(mpg/patch {:default-map :hstore}) ;; custom settings are merged with the defaults
;; valid settings:
    :data        - boolean, default true. auto-map maps and vectors?
    :datetime    - boolean, default true. auto-map java.time.{LocalDate, Instant, ZonedDateTime} ?
    :default-map - keyword. one of :json, :hstore. Default :json
    :default-time-zone - string or keyword describing a timezone, default: UTC
                         see https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html
```

## Timezone discussion.

The Java 8 Time API enables us to correctly handle Dates and Times. In
order to behave correctly, we need to know what timezone your database
is using by default for timestamp columns (which do not track timezone info).

We *strongly* recommend you use the default, UTC. The only reason you
should not do this is if you are certain that the database is in
another time zone, for compatibility with existing databases. Don't
create new databases in timezones other than UTC.

Dates and times are an extremely complicated area. Where possible. Use
UTC where possible and preferably make your database fields
timestamptz (the same size, just easier to get correct)

## Limitations

The current clojure.java.jdbc interface imposes some limitations on us.

1. You must use clojure.java.jdbc or a library that does to benefit from this library
2. When not using prepared statements, we cannot save a vector as an array type (we therefore use json)
3. When not using prepared statements, you must choose between storing maps as json or hstore (default: json)
4. In order to reliably convert un-zoned timestamps, we need a default
   timezone to treat them as belonging to (default: UTC)

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
