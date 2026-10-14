# jEAP Process Archive Service

The jEAP Process Archive Service (PAS) is a reusable microservice template for archiving artifacts that
pertain to a business process — for example for audit purposes or because of business requirements. A
PAS instance is built from the `jeap-process-archive-*` libraries and instantiated per business
application.

Archiving is triggered by Kafka messages (domain events and commands). The data to archive is either
derived from the message payload (event-carried state transfer) or fetched from a business service over
its REST API (event notification). The PAS stores the data in an S3-compatible object store, optionally
encrypts it, records retention via S3 lifecycle rules and object locks, and publishes a
`SharedArchivedArtifactVersionCreatedEvent` for every archived artifact.

## Documentation

### Getting started

- [Getting started](docs/getting-started.md) — set up a PAS instance

### Architecture

- [Architecture](docs/architecture.md) — context, runtime view, modules

### Configuration

- [Configuration](docs/configuration.md) — `jeap.processarchive.*`

### Service-specific topics

- [Consuming messages](docs/consuming-messages.md) — `messages.json`, providers, multiple artifacts
- [Archive-data REST interface](docs/archive-data-rest-interface.md) — remote data, Avro over REST
- [Archive types](docs/archive-types.md) — Avro & config-defined, validation
- [Archive Type Registry](docs/archive-type-registry.md) — schema repository, plugins
- [Object storage](docs/object-storage.md) — S3 connection, buckets, strategy
- [Encryption](docs/encryption.md) — data at rest, encrypted Kafka records
- [Archived artifacts & events](docs/archived-artifacts.md) — listener, event, idempotence, feature flags
- [OpenSearch integration](docs/opensearch.md)
- [Reading archived data](docs/reading-archived-data.md)

### Operations

- [Data expiration & locking](docs/data-expiration-and-locking.md) — retention, lifecycle, object lock
- [Backfill jobs](docs/backfill.md)
- [Metrics](docs/metrics.md)

### Development

- [Development](docs/local-development.md) — local development & testing

## License

This repository is Open Source Software licensed under the [Apache License 2.0](./LICENSE).

## Note

This repository is part of the open source distribution of jEAP. See
[github.com/jeap-admin-ch/jeap](https://github.com/jeap-admin-ch/jeap) for more information.
