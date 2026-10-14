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
- A `HashProvider` Spring bean for payload hashes and storage-object names — required by the
  object-storage adapter; see [Hash provider](object-storage.md#hash-provider).
- Optionally `ArchiveDataCondition`, `MessageCorrelationProvider`,
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

## Complete integration examples

The following source examples include the complete build, configuration and plugin implementations.
Use them with the version of the service you integrate; the examples evolve with their repositories.

- [jme-archive-type-registry](https://github.com/jme-admin-ch/jme-archive-type-registry)
- [jme-crypto-example](https://github.com/jme-admin-ch/jme-crypto-example)
- [jme-process-archive-example](https://github.com/jme-admin-ch/jme-process-archive-example)
- [jme-process-archive-example / README.md](https://github.com/jme-admin-ch/jme-process-archive-example/blob/main/README.md)
- [jme-process-archive-example / jme-process-archive-resource-service/pom.xml](https://github.com/jme-admin-ch/jme-process-archive-example/blob/main/jme-process-archive-resource-service/pom.xml)
- [jme-process-archive-example / jme-process-archive-resource-service / ArchiveController.java](https://github.com/jme-admin-ch/jme-process-archive-example/blob/main/jme-process-archive-resource-service/src/main/java/ch/admin/bit/jeap/jme/processarchive/resource/web/ArchiveController.java)
- [jme-process-archive-example / jme-process-archive-resource-service / DiagramController.java](https://github.com/jme-admin-ch/jme-process-archive-example/blob/main/jme-process-archive-resource-service/src/main/java/ch/admin/bit/jeap/jme/processarchive/resource/web/DiagramController.java)
- [jme-process-archive-example / jme-process-archive-service/pom.xml](https://github.com/jme-admin-ch/jme-process-archive-example/blob/main/jme-process-archive-service/pom.xml)
- [jme-process-archive-example / jme-process-archive-service / ProcessArchiveMessageContracts.java](https://github.com/jme-admin-ch/jme-process-archive-example/blob/main/jme-process-archive-service/src/main/java/ch/admin/bit/jeap/jme/processarchive/service/ProcessArchiveMessageContracts.java)
- [jme-process-archive-example / jme-process-archive-service / DecreeConverter.java](https://github.com/jme-admin-ch/jme-process-archive-example/blob/main/jme-process-archive-service/src/main/java/ch/admin/bit/jeap/jme/processarchive/service/indextype/DecreeConverter.java)
- [jme-process-archive-example / jme-process-archive-service / DecreeCorrelationProvider.java](https://github.com/jme-admin-ch/jme-process-archive-example/blob/main/jme-process-archive-service/src/main/java/ch/admin/bit/jeap/jme/processarchive/service/provider/DecreeCorrelationProvider.java)
- [jme-process-archive-example / jme-process-archive-service / DecreeCreatedDataProvider.java](https://github.com/jme-admin-ch/jme-process-archive-example/blob/main/jme-process-archive-service/src/main/java/ch/admin/bit/jeap/jme/processarchive/service/provider/DecreeCreatedDataProvider.java)
- [jme-process-archive-example / jme-process-archive-service / DecreeDocumentCreatedArchiveDataReferenceProvider.java](https://github.com/jme-admin-ch/jme-process-archive-example/blob/main/jme-process-archive-service/src/main/java/ch/admin/bit/jeap/jme/processarchive/service/provider/DecreeDocumentCreatedArchiveDataReferenceProvider.java)
- [jme-process-archive-example / jme-process-archive-service / JmeArchiveTypeProvider.java](https://github.com/jme-admin-ch/jme-process-archive-example/blob/main/jme-process-archive-service/src/main/java/ch/admin/bit/jeap/jme/processarchive/service/provider/JmeArchiveTypeProvider.java)
- [jme-process-archive-example / jme-process-archive-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](https://github.com/jme-admin-ch/jme-process-archive-example/blob/main/jme-process-archive-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
- [jme-process-archive-example / jme-process-archive-service/src/main/resources/processarchive/indextypes.json](https://github.com/jme-admin-ch/jme-process-archive-example/blob/main/jme-process-archive-service/src/main/resources/processarchive/indextypes.json)
- [jme-process-archive-example / jme-process-archive-service/src/main/resources/processarchive/messages.json](https://github.com/jme-admin-ch/jme-process-archive-example/blob/main/jme-process-archive-service/src/main/resources/processarchive/messages.json)
- [jme-process-archive-example / docker](https://github.com/jme-admin-ch/jme-process-archive-example/tree/main/docker)
- [jme-process-archive-example / jme-process-archive-service](https://github.com/jme-admin-ch/jme-process-archive-example/tree/main/jme-process-archive-service)

## Related

- [Architecture](architecture.md)
- [Consuming messages](consuming-messages.md)
- [Object storage](object-storage.md)
- [Configuration reference](configuration.md)
- [Local development & testing](local-development.md)
