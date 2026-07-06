# AGENTS.md

Guidance for AI coding agents working **in this repository**. To *use* the Process Archive Service in a
business application, read [README.md](README.md) and the [docs/](docs/) folder instead.

## Build Commands

```bash
./mvnw clean install                            # Full build with tests
./mvnw clean install -DskipTests                # Build without tests
./mvnw test -pl <module> -Dtest=ClassName       # Run a single unit test in a module
./mvnw verify -pl <module> -Dit.test=ClassName  # Run a single integration test
```

Unit tests (`*Test.java`) run in the `test` phase; integration tests (`*IT.java`) run in the `verify` phase.

## Architecture

A **Spring Boot service template for archiving process data**, triggered by Kafka messages (domain
events and commands), built on the jEAP framework with a plugin architecture. A PAS instance is created
per business application (see [docs/getting-started.md](docs/getting-started.md)).

### Data flow (live path)

```
Kafka message → KafkaMessageListener (adapter-kafka)
  → MessageReceiver (domain): findByName(messageType) → List<MessageArchiveConfiguration>
  → MessageArchiveService applies EVERY matching configuration (one artifact each)
  → ArchiveDataFactory extracts ArchiveData (payload provider or remote HTTP)
  → validate against the Avro schema (type-registry)
  → store via ArchiveDataObjectStore (adapter-objectstorage, S3), optional encryption (adapter-crypto)
  → notify ArtifactArchivedListener(s); publish SharedArchivedArtifactVersionCreatedEvent (adapter-kafka)
```

### Multiple artifacts per message

Several configurations may be registered for the same message/topic in `messages.json`; each produces
one artifact, so one message can archive multiple artifacts. Key rules:

- `MessageArchiveConfigurationRepository.findByName` returns a `List`; `MessageReceiver` applies all.
- Each artifact's idempotence id is `messageType_<sha256-hex>` where the hash covers the message
  idempotence id and the artifact discriminator (system, schema, referenceId, optional version) with
  length-prefixed fields (see `ArchiveArtifactIdempotenceId`); `MessageArchiveService` fails fast if
  two configurations of one message produce the same idempotence id.
- When a message has multiple configurations, each must declare a non-blank, unique `id` (enforced at
  config load in `JsonMessageArchiveConfigurationRepository`). Single-config messages may omit `id`.

### Backfill

Backfill (`domain/backfill`, `adapter-rest-api`, `adapter-db`, `adapter-kafka/backfill`) re-archives
existing data via a YAML REST job → one `CreateArtifactCommand` per reference → `BackfillCommandProcessor`.
When a message has multiple remote-data configurations, the request selects one via `config-id` (the
configuration's `id`); the resolved `configId` is stored on the backfill job (`backfill_job.config_id`)
and read back at processing time. `CreateArtifactCommand` is an **external** message-type artifact, so
its payload is not extended here. See [docs/backfill.md](docs/backfill.md).

### Key modules

- **`domain`** — archive orchestration, configuration lookup, schema validation, backfill domain
- **`plugin-api`** — SPI: `MessageArchiveDataProvider`, `ArchiveDataReferenceProvider`, `ReferenceProvider`,
  `ArtifactArchivedListener`, `ArchiveDataCondition`, `MessageCorrelationProvider`, `HashProvider`,
  `ObjectStorageStrategy`, `ArchiveTypeProvider`
- **`config-repository`** — loads `messages.json`; **`config-type-repository`** — config-defined archive types
- **`adapter-kafka`** — message ingestion, artifact-event & backfill-command producer/consumer
- **`adapter-objectstorage`** (S3) · **`adapter-crypto`** (jEAP Crypto) · **`adapter-db`** (backfill) · *
  *`adapter-rest-api`** (backfill REST) · **`adapter-opensearch`** (optional index)
- **`remote-data-provider`** — HTTP client for remote archive data · **`web`** — Avro-over-REST converter
- **`type-registry`** + **`type-registry-maven-plugin`** + **`avro-maven-plugin`** / **`avro-validator`** — archive
  types & Avro
- **`service`** — Spring Boot entry point · **`service-instance`** — deployable instance parent

### Configuration model

`MessageArchiveConfiguration` (abstract) has two variants: `PayloadDataMessageArchiveConfiguration`
(data from the message payload) and `RemoteDataMessageArchiveConfiguration` (data fetched from a remote
HTTP service). Each carries an optional `id`. Configurations are looked up per message name via
`MessageArchiveConfigurationRepository`; the file location is overridable via
`jeap.processarchive.configuration.location` (default `classpath:/processarchive/messages.json`).

### Key conventions

- Package root: `ch.admin.bit.jeap.processarchive`; Spring properties under `jeap.processarchive.*`.
- Messages use Avro and extend jEAP `Message` base types.
- Feature flags via Togglz (`FeatureManager`) gate individual archive configurations.
- Test profile `ObjectStorageConfiguration.JEAP_PAS_TEST_INMEMORY_PROFILE` provides in-memory storage.
- Integration tests use `KafkaIntegrationTestBase`, WireMock, Awaitility and TestContainers.
- Follow the [jEAP documentation principles](https://jeap-admin-ch.github.io/docs/documenting-jeap) for
  `docs/` (flat layout, README landing page, Mermaid, valid MDX).
