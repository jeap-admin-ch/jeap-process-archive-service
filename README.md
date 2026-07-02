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

Start with [Getting started](docs/getting-started.md), then follow the links below.

| Topic                                                                     | File                                                                       |
|---------------------------------------------------------------------------|----------------------------------------------------------------------------|
| Architecture (context, runtime view, modules)                             | [docs/architecture.md](docs/architecture.md)                               |
| Getting started (set up a PAS instance)                                   | [docs/getting-started.md](docs/getting-started.md)                         |
| Consuming messages (`messages.json`, providers, multiple artifacts)       | [docs/consuming-messages.md](docs/consuming-messages.md)                   |
| Archive-data REST interface (remote data, Avro over REST)                 | [docs/archive-data-rest-interface.md](docs/archive-data-rest-interface.md) |
| Archive types (Avro & config-defined, validation)                         | [docs/archive-types.md](docs/archive-types.md)                             |
| Archive Type Registry (schema repository, plugins)                        | [docs/archive-type-registry.md](docs/archive-type-registry.md)             |
| Object storage (S3 connection, buckets, strategy)                         | [docs/object-storage.md](docs/object-storage.md)                           |
| Data expiration & locking (retention, lifecycle, object lock)             | [docs/data-expiration-and-locking.md](docs/data-expiration-and-locking.md) |
| Encryption (data at rest, encrypted Kafka records)                        | [docs/encryption.md](docs/encryption.md)                                   |
| Archived artifacts & events (listener, event, idempotence, feature flags) | [docs/archived-artifacts.md](docs/archived-artifacts.md)                   |
| OpenSearch integration                                                    | [docs/opensearch.md](docs/opensearch.md)                                   |
| Backfill jobs                                                             | [docs/backfill.md](docs/backfill.md)                                       |
| Reading archived data                                                     | [docs/reading-archived-data.md](docs/reading-archived-data.md)             |
| Metrics                                                                   | [docs/metrics.md](docs/metrics.md)                                         |
| Configuration reference (`jeap.processarchive.*`)                         | [docs/configuration.md](docs/configuration.md)                             |
| Local development & testing                                               | [docs/local-development.md](docs/local-development.md)                     |

## License

This repository is Open Source Software licensed under the [Apache License 2.0](./LICENSE).

## Note

This repository is part of the open source distribution of jEAP. See
[github.com/jeap-admin-ch/jeap](https://github.com/jeap-admin-ch/jeap) for more information.
