# Getting started

This page shows how to set up a Process Archive Service (PAS) instance for a business application. For
the bigger picture see [Architecture](architecture.md). A PAS instance is a small Spring Boot service
that consumes trigger messages and archives data; you provide configuration and a few plugin
implementations.

The `jme-process-archive-example` project is a complete reference instance and the recommended starting
template.

## 1. Create the Maven instance

Use `jeap-process-archive-service-instance` as the Maven parent:

```xml
<parent>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-process-archive-service-instance</artifactId>
    <version>use-the-latest-version-here</version>
    <relativePath/>
</parent>
```

In a multi-module project you cannot use this parent directly; in that case add the
`jeap-process-archive-service` and `jeap-process-archive-plugin-api` dependencies explicitly and make
sure the `jeap-spring-boot-parent` version used by the project matches the one used by the PAS
dependencies.

## 2. Add dependencies

At minimum a PAS instance depends on the application module and the plugin API:

```xml
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-process-archive-service</artifactId>
</dependency>
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-process-archive-plugin-api</artifactId>
</dependency>
```

Add further modules for optional features:

- The message-type Java bindings for every trigger message you consume.
- The [Archive Type](archive-types.md) Java bindings for every Avro archive type you validate.
- `jeap-process-archive-web` if the source service serves archive data as binary Avro over REST.
- `jeap-process-archive-adapter-opensearch` for the optional [OpenSearch](opensearch.md) integration.
- `jeap-process-archive-adapter-db` (plus a database) if you enable [backfill](backfill.md).
- A jEAP Crypto starter (`jeap-crypto-aws-kms-starter`, `jeap-crypto-vault-starter`, …) if you
  [encrypt](encryption.md) archived data.

When using the service-instance parent, versions are managed by dependency management.

## 3. Configure the service

A PAS instance is configured through:

- `src/main/resources/processarchive/messages.json` — the messages that trigger archiving and how to
  determine the data to archive. See [Consuming messages](consuming-messages.md).
- `application-<env>.yml` — jEAP Messaging (Kafka), the [S3 object storage](object-storage.md)
  connection, the [archived artifact event](archived-artifacts.md) topic, and optionally encryption,
  OpenSearch and backfill.
- A jEAP Messaging contract for every consumed/produced message (see step 5).

## 4. Implement the plugins

Depending on the archiving mode you implement a small number of
[plugin-api](consuming-messages.md#plugin-interfaces) interfaces as Spring beans or referenced classes:

- `ArchiveDataReferenceProvider` and/or `MessageArchiveDataProvider` — determine what to archive.
- Optionally `ArchiveDataCondition`, `MessageCorrelationProvider`, `HashProvider`,
  `ObjectStorageStrategy`, `ArtifactArchivedListener`.
- For Avro archive types, an `ArchiveTypeProvider` that lists the archive-type versions on the
  classpath (see [Archive types](archive-types.md)).

## 5. Declare message contracts

Every consumed and produced message needs a jEAP Messaging contract. The PAS derives consumer contracts
from `messages.json` via `@JeapMessageConsumerContractsByTemplates`; declare the produced event (and, if
using backfill, the command) explicitly:

```java
@JeapMessageProducerContract(value = SharedArchivedArtifactVersionCreatedEvent.TypeRef.class,
        topic = "jme-process-archive-artifactversioncreated")
@JeapMessageConsumerContractsByTemplates
interface ProcessArchiveMessageContracts {
}
```

## 6. Run it

For local development you need Kafka and an S3 object store (the example project starts these with
Docker, using RustFS as the object store). See [Local development & testing](local-development.md).

## Related

- [Architecture](architecture.md)
- [Consuming messages](consuming-messages.md)
- [Object storage](object-storage.md)
- [Configuration reference](configuration.md)
- [Local development & testing](local-development.md)
