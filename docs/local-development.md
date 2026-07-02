# Local development & testing

## Runtime dependencies

For local development and testing the PAS needs its runtime environment — in particular Kafka and an
S3-compatible object store. If archived data must be [encrypted](encryption.md), a key management system
(e.g. Vault) is also required.

## In-memory object store for tests

For integration tests the PAS provides a simple in-memory implementation of its `ObjectStorageRepository`,
activated with the Spring profile `ObjectStorageConfiguration.JEAP_PAS_TEST_INMEMORY_PROFILE`. This lets
tests exercise the full archiving flow without a real S3 backend. Integration tests typically also use
an embedded Kafka broker and (for backfill) a Testcontainers PostgreSQL database.

## Isolating the archive configuration in tests

The location of the archive configuration file can be overridden with
`jeap.processarchive.configuration.location` (default `classpath:/processarchive/messages.json`). A test
can point this at its own resource to exercise a specific configuration in isolation.

## Building

```bash
./mvnw clean install                             # full build with tests
./mvnw clean install -DskipTests                 # build without tests
./mvnw test -pl <module> -Dtest=ClassName        # a single unit test
./mvnw verify -pl <module> -Dit.test=ClassName   # a single integration test
```

Unit tests (`*Test`) run in the `test` phase; integration tests (`*IT`) run in the `verify` phase.

## Related

- [Getting started](getting-started.md)
- [Consuming messages](consuming-messages.md)
- [Configuration reference](configuration.md)
