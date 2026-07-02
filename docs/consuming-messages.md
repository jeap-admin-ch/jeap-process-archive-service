# Consuming messages

The PAS archives data in reaction to Kafka messages (domain events or commands). Which messages trigger
archiving, and how the data to archive is determined, is declared in
`src/main/resources/processarchive/messages.json`.

> Before PAS version 11 this file was named `events.json` and the keys started with `event*` /
> `domainEvent*` instead of `message*`. The old file name and keys are still supported for backward
> compatibility.

## Declaring messages

Each entry identifies a message and how to archive it:

| Attribute             | Required    | Description                                                                                                                                                                                                                        | Example                                                                           |
|-----------------------|-------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| `id`                  | conditional | Identifier of this configuration, unique per message. Optional for a single configuration; **mandatory and unique when a message has multiple configurations**. Used to address a configuration (e.g. for [backfill](backfill.md)) | `decree-document`                                                                 |
| `messageName`         | yes         | Name of the consumed message                                                                                                                                                                                                       | `JmeDecreeCreatedEvent`                                                           |
| `topicName`           | yes         | Topic to consume the message from                                                                                                                                                                                                  | `jme-process-archive-decreecreated`                                               |
| `clusterName`         | no          | Logical Kafka cluster name (see multi-cluster support). Defaults to the default cluster                                                                                                                                            | `aws`                                                                             |
| `condition`           | no          | Fully-qualified `ArchiveDataCondition` class; data is only archived when it returns `true` for the message                                                                                                                         | `ch.admin.bit.jeap.test.processarchive.TestCondition`                             |
| `correlationProvider` | no          | Fully-qualified `MessageCorrelationProvider` class; supplies the `processId` of the archived record. Defaults to the message's `processId`                                                                                         | `ch.admin.bit.jeap.jme.processarchive.service.provider.DecreeCorrelationProvider` |
| `featureFlag`         | no          | Feature flag gating publication of the archived artifact (see [Archived artifacts](archived-artifacts.md))                                                                                                                         | `FEATURE_DECREE_CREATED`                                                          |

Plus the attributes that determine the data to archive (see below): either
`archiveDataReferenceProvider` + `uri` (+ `oauthClientId`) for remote data, or `messageArchiveDataProvider`
for payload data.

### Multiple artifacts per message

Since PAS 16.0.0 you may register **several configurations for the same `messageName`** (they must all
use the same `topicName`/`clusterName`). Each configuration produces one artifact, so one message can
archive multiple artifacts. Each artifact must be distinct (distinct system/schema/reference id/version);
the PAS validates this before storing and fails fast otherwise. See
[Archived artifacts & events](archived-artifacts.md#idempotence) for how idempotence ids are derived.

When a message has multiple configurations, each configuration must declare a non-blank, unique `id`
(config validation fails otherwise). The `id` lets a specific configuration be addressed — in particular
by a [backfill](backfill.md) job via its `config-id`.

Earlier PAS versions allowed only one configuration per message type.

## Determining the data to archive

There are two mechanisms, chosen per message.

### Event notification (remote data)

If the message only carries a *reference* (id and optional version) of a record, the PAS fetches the
data from a source service over REST. Provide an `ArchiveDataReferenceProvider` that extracts the
reference from the message and a `uri` template for the source endpoint:

```json
{
  "messageName": "JmeDecreeDocumentCreatedEvent",
  "topicName": "jme-process-archive-decreedocumentcreated",
  "archiveDataReferenceProvider": "ch.admin.bit.jeap.jme.processarchive.service.provider.DecreeDocumentCreatedArchiveDataReferenceProvider",
  "uri": "${decree-document.archive.uri}",
  "oauthClientId": "jme-process-archive-resource-service"
}
```

| Attribute                      | Required | Description                                                                                                                  |
|--------------------------------|----------|------------------------------------------------------------------------------------------------------------------------------|
| `archiveDataReferenceProvider` | yes      | Fully-qualified `ArchiveDataReferenceProvider` class                                                                         |
| `uri`                          | yes      | Source endpoint with a `{id}` parameter (and optional `{version}` parameter for versioned records)                           |
| `oauthClientId`                | no*      | OAuth2 client id (from the Spring OAuth2 config) used to obtain a token; if omitted the API is called without authentication |

```java
public class DecreeDocumentCreatedArchiveDataReferenceProvider
        implements ArchiveDataReferenceProvider<JmeDecreeDocumentCreatedEvent> {
    @Override
    public ArchiveDataReference getReference(JmeDecreeDocumentCreatedEvent event) {
        return ArchiveDataReference.builder()
                .id(event.getReferences().getNewDecreeDocument().getId())
                .build();
    }
}
```

The source service must implement the [standardized archive-data REST interface](archive-data-rest-interface.md).

### Event-carried state transfer (payload data)

If the message already contains all the data to archive, provide a `MessageArchiveDataProvider` that
builds the `ArchiveData` from the message payload — no REST call is made:

```json
{
  "messageName": "JmeDecreeCreatedEvent",
  "topicName": "jme-process-archive-decreecreated",
  "messageArchiveDataProvider": "ch.admin.bit.jeap.jme.processarchive.service.provider.DecreeCreatedDataProvider"
}
```

```java
public class DecreeCreatedDataProvider implements MessageArchiveDataProvider<JmeDecreeCreatedEvent> {
    @Override
    public ArchiveData getArchiveData(JmeDecreeCreatedEvent event) {
        return ArchiveData.builder()
                .referenceId(event.getReferences().getNewDecree().getId())
                .system("JME")
                .schema("Decree")
                .schemaVersion(3)
                .contentType("avro/binary")
                .payload(/* serialize the avro archive type */)
                .build();
    }
}
```

`system`, `schema` and `schemaVersion` must match a known [archive type](archive-types.md); for
`avro/binary` content the payload is validated against the archive type's Avro schema.

## Example configuration

```json
{
  "messages": [
    {
      "messageName": "JmeDecreeDocumentCreatedEvent",
      "topicName": "jme-process-archive-decreedocumentcreated",
      "archiveDataReferenceProvider": "ch.admin.bit.jeap.jme.processarchive.service.provider.DecreeDocumentCreatedArchiveDataReferenceProvider",
      "uri": "${decree-document.archive.uri}",
      "featureFlag": "FEATURE_DOCUMENT_CREATED"
    },
    {
      "messageName": "JmeDecreeCreatedEvent",
      "topicName": "jme-process-archive-decreecreated",
      "messageArchiveDataProvider": "ch.admin.bit.jeap.jme.processarchive.service.provider.DecreeCreatedDataProvider",
      "correlationProvider": "ch.admin.bit.jeap.jme.processarchive.service.provider.DecreeCorrelationProvider"
    },
    {
      "messageName": "JmeDiagramVersionCreatedEvent",
      "topicName": "jme-process-archive-diagramversioncreated",
      "archiveDataReferenceProvider": "ch.admin.bit.jeap.jme.processarchive.service.provider.DiagramVersionCreatedArchiveDataReferenceProvider",
      "condition": "ch.admin.bit.jeap.jme.processarchive.service.provider.ArchiveDiagramCondition",
      "uri": "${diagram.archive.uri}"
    }
  ]
}
```

## Plugin interfaces

The relevant SPI interfaces live in `jeap-process-archive-plugin-api`. Referenced provider classes are
instantiated by the PAS from their fully-qualified name in `messages.json`; listeners, hash providers
and object-storage strategies are picked up as Spring beans.

| Interface                         | Role                                                                            |
|-----------------------------------|---------------------------------------------------------------------------------|
| `ArchiveDataReferenceProvider<M>` | Extract the reference (id/version) for the remote-data flow                     |
| `MessageArchiveDataProvider<M>`   | Build `ArchiveData` directly from the message payload                           |
| `ArchiveDataCondition<M>`         | Decide whether a message should be archived                                     |
| `MessageCorrelationProvider<M>`   | Supply the `processId` of the archived record                                   |
| `ArtifactArchivedListener`        | Receive the completed [archived artifact](archived-artifacts.md)                |
| `HashProvider`                    | Compute the payload hash stored in object metadata                              |
| `ObjectStorageStrategy`           | Choose the bucket/key for an artifact (see [Object storage](object-storage.md)) |
| `ArchiveTypeProvider`             | Register Avro [archive types](archive-types.md) on the classpath                |

## Related

- [Archive-data REST interface](archive-data-rest-interface.md)
- [Archive types](archive-types.md)
- [Archived artifacts & events](archived-artifacts.md)
- [Object storage](object-storage.md)
- [Configuration reference](configuration.md)
