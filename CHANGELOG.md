# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [9.4.0] - 2025-09-29

### Changed

- Update parent from 27.2.0 to 27.3.0

## [9.3.0] - 2025-09-19

### Changed

- Update parent from 27.1.1 to 27.2.0

## [9.2.0] - 2025-09-11

### Changed

- Update parent from 26.76.0 to 27.1.1

## [9.1.0] - 2025-09-02

### Changed

- Update parent from 26.75.1 to 26.76.0

## [9.0.0] - 2025-08-29

### Changed

- Note that this a major version update! Archive types are no longer retrieved from the archive type registry
  during the buildphase, but rather published as maven artifacts. They can then simply be added as a dependency
  to the PAS and other services. See the updated documentation for details.

## [8.20.0] - 2025-08-26

### Changed

- Update parent from 26.73.0 to 26.74.0

## [8.19.0] - 2025-08-15

### Changed

- Update parent from 26.72.0 to 26.73.0

## [8.18.0] - 2025-08-05

### Changed

- Update parent from 26.71.0 to 26.72.0
- update file-management from 3.0.0 to 3.2.0
- update json-schema-validator from 1.4.0 to 1.5.8
- update org.eclipse.jgit from 6.9.0.202403050737-r to 7.3.0.202506031305-r
- update maven.api from 3.8.8 to 3.9.11
- update maven-plugin-plugin from 3.15.0 to 3.15.1
- remove commons-io from pom.xml
- update wiremock from 3.12.0 to 3.13.1
- update wiremock-spring-boot from 3.7.0 to 3.10.0

## [8.17.0] - 2025-07-25

### Changed

- Update parent from 26.68.0 to 26.71.0

## [8.16.0] - 2025-07-08

### Changed

- Update parent from 26.67.0 to 26.68.0

## [8.15.0] - 2025-07-03

### Changed

- Support for Git token authentication when accessing a remote repository in the archive type registry maven plugins
- Support for using the system's Git installation when accessing a remote repository without a Git token in the archive type registry maven plugins
- Update parent from 26.63.0 to 26.67.0

## [8.14.0] - 2025-07-03

### Changed

- ArtifactArchivedListener supports feature flags
- Update parent from 26.63.0 to 26.66.1

## [8.13.0] - 2025-06-19

### Changed

- Update parent from 26.61.0 to 26.63.0
- Update maven.api.version from 3.8.1 to 3.8.8

## [8.12.0] - 2025-06-17

### Changed

- Update parent from 26.57.0 to 26.61.0

## [8.11.0] - 2025-06-13

### Changed

- Update parent from 26.55.0 to 26.57.0

## [8.10.0] - 2025-06-06

### Changed

- Update parent from 26.43.2 to 26.55.0

## [8.9.0] - 2025-04-15

### Changed

- Update parent from 26.42.0 to 26.43.2

## [8.8.0] - 2025-04-01

### Changed

- Update parent from 26.41.3 to 26.42.0

## [8.7.1] - 2025-04-01

### Changed

- Update parent from 26.35.0 to 26.41.3

## [8.7.0] - 2025-03-07

### Changed

- Configure proxy to work around the issue https://github.com/aws/aws-sdk-java-v2/issues/4728 which is coming with the aws sdk update
- Update parent from 26.33.0 to 26.35.0

## [8.6.0] - 2025-03-06

### Changed

- Update parent from 26.31.0 to 26.33.0

## [8.5.0] - 2025-03-05

### Changed

- Update parent from 26.24.2 to 26.31.0

## [8.4.0] - 2025-02-14

### Changed

- Prepare repository for Open Source distribution

## [8.3.0] - 2025-02-13

### Changed

- Update parent from 26.23.0 to 26.24.2
- Disable license plugins for service instances

## [8.2.1] - 2025-02-12

### Changed

- Configured the javadoc plugin in the instance module to not fail on errors in javadoc comments.

## [8.2.0] - 2025-02-10

### Changed

- Update parent from 26.22.3 to 26.23.0

## [8.1.0] - 2025-01-15

### Changed

- Added the module jeap-process-archive-service-instance which will instantiate a PAS instance when used as parent project.

## [8.0.0] - 2025-01-10

### Changed

- At startup, the PAS validates contracts against the template to ensure that all necessary contracts are present. Otherwise, the startup will fail.

## [7.12.0] - 2024-12-20

### Changed

- Update parent from 26.21.1 to 26.22.2

## [7.11.1] - 2024-12-20

### Changed

- Remove arden-file dependency

## [7.11.0] - 2024-12-19

### Changed

- Update parent from 26.5.0 to 26.21.1

## [7.10.0] - 2024-10-31

### Changed

- Update parent from 26.4.0 to 26.5.0

## [7.9.1] - 2024-10-22

### Changed

- Update maven.api.version to 3.8.1 (CVE-2021-26291)
- Update commons-io to 2.14 (CVE-2024-47554)

## [7.9.0] - 2024-10-17

### Changed

- Update parent from 26.3.0 to 26.4.0

## [7.8.0] - 2024-09-20

### Changed

- Update parent from 26.0.0 to 26.3.0

## [7.7.0] - 2024-09-13

### Changed

- Add hashReferenceId to HashProvider interface

## [7.6.0] - 2024-09-06

### Changed

- Update parent from 25.4.0 to 26.0.0

## [7.5.0] - 2024-08-22

### Changed

- Update parent from 25.1.0 to 25.4.0

## [7.4.1] - 2024-07-24

### Changed

- Use env var AWS_REGION for S3 client if set

## [7.4.0] - 2024-07-16

### Changed

- Update parent from 23.16.0 to 24.5.0, which includes upgrade to Spring Boot 3.3.1.

## [7.3.0] - 2024-05-06

### Changed

- Update parent from 23.12.0 to 23.16.0
- Add a correlationProvider interface in order to implement how the processId is read from the received message

## [7.2.0] - 2024-03-28

### Changed

- Support conditional archival of data

## [7.1.0] - 2024-03-28

### Changed

- Update parent from 23.10.5 to 23.12.0

## [7.0.0] - 2024-03-22

### Changed

- Produce SharedArchivedArtifactVersionCreatedEvent in PAS
- Update parent to 23.10.5

## [6.10.1] - 2024-03-11

### Changed

- Update parent to 23.10.4 (latest jeap-crypto version with some configuration improvements)

## [6.10.0] - 2024-03-05

### Changed

- Support encryption of archive data by reference an encryption key by its name. This makes the PAS encryption
  KMS-agnostic and no longer Vault-specific.

## [6.9.0] - 2024-03-04

### Changed

- Avro-Maven-Plugin: the property enableDecimalLogicalType of avro compiler is configurable

## [6.8.0] - 2024-02-27

### Changed

- Upgraded jeap parent from 23.0.0 to 23.6.1 
- Switched from WebClient to RestClient and removed webflux and reactor dependencies.

## [6.7.0] - 2024-02-21

### Changed

- Add compatibilityMode to archive type versions in descriptor
- Validate compatibility of avro schemas between versions

## [6.6.0] - 2024-02-01

### Changed

- Update parent from 22.5.0 to 23.0.0
- Remove support for v1 messaging contracts

## [6.5.0] - 2024-01-25

### Changed

- Update parent from 22.2.3 to 22.5.0

## [6.4.0] - 2024-01-23

### Changed

- Update parent from 22.1.0 to 22.2.3

## [6.3.1] - 2024-01-17

### Changed

- Make ObjectLockMode configurable

## [6.3.0] - 2024-01-16

### Changed

- Update parent from 22.0.0 to 22.1.0

## [6.2.2] - 2024-01-12

### Fixed

- Make TimedS3Client compatible with AWS S3
 
## [6.2.1] - 2024-01-10

### Fixed

- Improve error handling in case no lifecycle policies are present on bucket

## [6.2.0] - 2024-01-09

### Changed

- Update parent from 21.2.0 to 21.3.3 (no bootstrap)
- add multi cluster support for events defined in event configuration

## [6.1.2] - 2023-12-22

### Changed

- Explicitly excluding apache client 

## [6.1.1] - 2023-12-21

### Changed

- Changing jeap-process-archive-adapter-objectstorage to use url-connection-client instead of apache http client

## [6.1.0] - 2023-12-15

### Changed

- Update parent from 21.0.0 to 21.2.0

## [6.0.1] - 2023-12-07

### Changed

- Support optional archiving of data by allowing null return values in ReferenceProvider

## [6.0.0] - 2023-11-21

### Changed

- upgrade to jeap-parent 21.0.0 with multi-cluster support in jeap-messaging

## [5.1.0] - 2023-11-10

### Changed

- Update parent from 20.0.0 to 20.10.1
- S3 region is configurable 
- S3 credentials (accessKey, secretKey) are now optional: if not configured, the default aws credentials are used

## [5.0.0] - 2023-08-16

### Changed

- Update to Spring Boot 3

## [4.16.0] - 2023-08-09

### Changed

- Update parent from 19.16.1 to 19.17.0

## [4.15.1] - 2023-08-08

### Fixed

- Removed non-working startup object storage connection check

## [4.15.0] - 2023-08-08

### Changed

- Update parent from 19.15.0 to 19.16.1

## [4.14.0] - 2023-07-12

### Changed

- Update to AWS SDK v2
- Update parent to 19.15.0

## [4.13.0] - 2023-06-22

### Changed

- Update parent to 19.13.0

## [4.12.0] - 2023-05-30

### Changed

- Update parent from 19.10.1 to 19.12.1

## [4.11.0] - 2023-04-21

### Changed

- Update parent from 19.7.1 to 19.10.1

## [4.10.0] - 2023-03-24

### Changed

- Change default prefix mode to DAY
- Add histogram metrics for S3 timers

## [4.9.0] - 2023-03-20

### Changed

- Update parent from 19.5.1 to 19.6.0
- Configure new timed metrics to analyze performance

## [4.8.0] - 2023-03-16

### Changed

- Update parent from 19.2.0 to 19.5.1
- Integration of jeap-crypto to encrypt the archive data

## [4.7.0] - 2023-02-21

### Changed

- Update parent from 18.6.1 to 19.2.0

## [4.6.1] - 2022-12-08

### Changed

- Update parent from 18.5.0 to 18.6.1
- Set the event version from contract file to validate the consumer contracts

## [4.6.0] - 2022-11-28

### Changed

- Update parent from 18.2.0 to 18.5.0

## [4.5.0] - 2022-10-31

### Changed

- Update parent from 18.0.0 to 18.2.0
- Added option to directly set non-proxy hosts on connections to the S3 storage.
- IdempotenceId of ArchiveArtifact is the name of the event concatenated with the idempotenceId of event

## [4.4.0] - 2022-10-06

### Changed

- Update parent from 17.3.0 to 18.0.0 (spring boot 2.7)

## [4.3.0] - 2022-09-21

### Changed

- Update parent from 17.2.2 to 17.3.0

## [4.2.0] - 2022-09-21

### Changed
- Archived objects are now locked in S3 in 'compliance' mode for the duration specified by the expiration days number of the corresponding archive type.

## [4.1.0] - 2022-09-13

### Changed
- Update parent from 16.0.1 to 17.2.2
- Remove component scan of jeap messaging and import kafka configurations separately in integration tests

## [4.0.0] - 2022-06-23

### Changed

- Added referenceIdType in artifactSchema
- Added referenceIdType in hash provider interface
- Moved ArchiveDataSchema from domain to plugin-api module

## [3.1.1] - 2022-06-03

### Fixed

- Fixed default lifecycle phase binding of ArchiveTypeDefinitionDownloadMojo

## [3.1.0] - 2022-06-02

### Added

- Compute name of storage object using the reference ID hash by default

## [3.0.0] - 2022-05-30

### Added

- Support lifecycle policies for object expirations (archive data time-to-live) in S3 and archive type descriptor

## [2.0.1] - 2022-05-16

### Changed

- Updated to jeap-parent 15.11.1 with jeap-messaging 3.7.1 (automated message version attribute value), no functional
  changes

## [2.0.0] - 2022-03-02

### Changed

- The process archive service no longer sets the 'javax.net.ssl.trustStore' property to the 'truststore.jks' resource on start-up.
  Therefore, you now will have to define the truststore for SSL connections yourself in your error handling service instance. For
  microservices in Cloudfoundry you can do this e.g. in the Cloudfoundry manifest file.
- Upgraded to jeap-spring-boot-parent 15.5.1.

## [1.7.1] - 2021-12-23

### Changed

- update to jeap-spring-boot-parent 15.2.0 (spring boot 2.6.2)

## [1.7.0] - 2021-12-02

### Added

- HTTP Converter for avro

## [1.6.2] - 2021-12-01

### Fixed

- Invalid javadoc in geneated avro records

## [1.6.1] - 2021-12-01

### Changed

- Require specific name for archive-types.json to avoid clash with registry plugin

## [1.6.0] - 2021-11-30

### Added

- Avro schema validation against archive type definition from registry

### Changed

- Schema version changed to integer

## [1.5.0] - 2021-11-30

### Added

- Added archive type registry validator maven plugin
- Added archive type avro maven plugin
- Added system to ArchiveData

## [1.4.1] - 2021-08-11

### Fixed

- Fixed default configuration of http timeout and Kafka polling.

## [1.4.0] - 2021-07-23

### Added

- Added support for versioned artifacts.

## [1.3.0] - 2021-07-09

### Changed

- Upgraded to jeap-spring-boot-parent 14.2.0 (optimized kafka config defaults)

## [1.2.2] - 2021-06-23

### Changed

- Upgraded to jeap-spring-boot-parent 14.0.4 (spring boot 2.5.1).

## [1.2.1] - 2021-06-15

### Fixed

- Setting truststore on startup to resource named 'truststore.jks'.

## [1.2.0] - 2021-06-11

### Changed

- Removed ignored configuration option 'retentionPeriod' from domain archive event configurations.
- The 'referenceId' path parameter in the URI used to fetch archive data is no longer just added to the end of the URI
  specified in the configuration. Instead the configured URI now has to specify a path template containing a single path
  parameter for the 'referenceId'.
- Trying to prevent archive data from getting overwritten.

## [1.1.1] - 2021-06-10

### Fixed

- Removed incorrectly present spring-boot-maven-ch.admin.bit.jeap.messaging.avro.plugin from pom.

## [1.1.0] - 2021-06-07

### Added

- Notify listener on new archived artifacts
- Read metadata/attributes from remote data provider
- Archiving artifacts to object storage.

## [1.0.0] - 2021-05-19

### Added

- Initial version
