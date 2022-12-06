<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# dart-ogurets-intellij-plugin Changelog

## [Unreleased]
### Added
- the generated `ogurets_run.dart` file will include required step defs from imported packages, enabling reuse of step definitions across multiple packages (#16)

## [2.0.0]
### Changed
- converted project to build using gradle 
- upgraded minimal compatibility from 2021.3 to 2022.3

## [1.3.0]
- support for 213 and fixing issue from user

## [1.1.5]
 - patch submission included code from groovy plugin which wasn't caught.

## [1.1.4]
 - incompatibilities for IDEA 2020.1

## [1.1.3]
 - incompatibilities for 193 of IDEA

## [1.1.2]
 - IDEA exception around read exception, adding in default parameter names. Fixing issues around alias names for stepdefs. 

## [1.1.0]
 - when the tests are in a non flutter top level project, the root folder checking for "test" or "test-driver" doesn't work.

## [1.0.1]
 - Removed dependency on java cucumber version that 2019.2 was complaining about.

## [1.0.0]
 - Updated to allow running features inside non test directories and without any dependencies other than Ogurets.
