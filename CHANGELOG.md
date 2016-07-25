# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [1.2.0] - 2016-07-25
### Fixed
- Issue with extension of protocol ISQLParameter where if the datatype could not be determined when resolving an IPersistentMap parameter (#19)
- Tests ensure extensions are present before running (#20)

## [1.1.0] - 2016-06-19
### Fixed
- Queries involving array type work consistently accross calls (#17)

## [1.0.0] - 2016-05-10
### Changed
- Better handling of `citext` type data (#13)
- Improved handling of `jdbc/insert!` operations (#14)

## [0.3.0] - 2016-04-28
### Changed
- Republish as `mpg`
