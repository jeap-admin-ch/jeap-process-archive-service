# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./mvnw clean install                          # Full build with tests
./mvnw clean install -DskipTests              # Build without tests
./mvnw test -pl <module> -Dtest=ClassName     # Run single unit test in a module
./mvnw verify -pl <module> -Dit.test=ClassName # Run single integration test
```

The CI uses Java 21 (`bit/eclipse-temurin:21`), but local JDK is OpenJDK 25. Integration tests (`*IT.java`) run during `verify` phase; unit tests (`*Test.java`) run during `test` phase.

## Architecture

This is a **Spring Boot service for archiving process data** triggered by Kafka messages (domain events and commands). It uses the jEAP framework (`jeap-spring-boot-parent:30.20.0`) and follows a plugin architecture.

### Data Flow

```
Kafka message → KafkaMessageListener (adapter-kafka)
  → MessageReceiver (domain) → MessageArchiveService (domain)
  → Lookup MessageArchiveConfiguration → ArchiveDataFactory
  → Extract data via MessageArchiveDataProvider (plugin SPI)
  → Validate against Avro schema
  → Store via ArchiveDataObjectStore (adapter-objectstorage, S3)
  → Optional encryption via ArchiveCryptoService (adapter-crypto)
  → Publish ArchivedArtifactVersionCreatedEvent via Kafka
```

### Key Modules

- **`domain`** — Core business logic: archive orchestration, configuration lookup, schema validation
- **`plugin-api`** — SPI interfaces that service instances implement: `MessageArchiveDataProvider`, `ArtifactArchivedListener`, `ArchiveDataCondition`, `ReferenceProvider`, `HashProvider`, `ObjectStorageStrategy`
- **`adapter-kafka`** — Kafka consumer/producer (message ingestion, artifact event publishing)
- **`adapter-objectstorage`** — S3 storage via AWS SDK
- **`adapter-crypto`** — Encryption via Crypto Vault
- **`remote-data-provider`** — HTTP client for fetching archive data from external services
- **`config-repository`** — Archive configuration repository
- **`type-registry`** — Archive type descriptors and Avro schema compilation
- **`type-registry-maven-plugin`** — Build-time validation of archive type descriptors
- **`avro-maven-plugin`** / **`avro-validator`** — Avro code generation and schema validation
- **`service`** — Spring Boot application entry point
- **`service-instance`** — Deployable service instance configuration

### Configuration Model

`MessageArchiveConfiguration` (abstract) has two variants:
- `PayloadDataMessageArchiveConfiguration` — extracts data from message payload
- `RemoteDataMessageArchiveConfiguration` — fetches data from a remote HTTP service

Configurations are looked up per message type via `ArchiveConfigurationRepository`.

### Key Conventions

- Package root: `ch.admin.bit.jeap.processarchive`
- Messages use Avro serialization and extend jEAP `Message` base types
- Feature flags via Togglz (`FeatureManager`) gate individual archive configurations
- Spring properties under `jeap.processarchive.*`
- Test profile `JEAP_PAS_TEST_INMEMORY_PROFILE` provides in-memory storage for tests
- Integration tests use `KafkaIntegrationTestBase`, WireMock, Awaitility, and TestContainers
