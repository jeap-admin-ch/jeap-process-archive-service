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
use the same `topicName`/`clusterName`). Each configuration can produce one artifact, so one message can
archive multiple artifacts. Each artifact must be distinct (distinct system/schema/reference id/version);
the PAS validates this before storing and fails fast otherwise. See
[Archived artifacts & events](archived-artifacts.md#idempotence) for how idempotence ids are derived.

When a message has multiple configurations, each configuration must declare a non-blank, unique `id`
(config validation fails otherwise). The `id` lets a specific configuration be addressed — in particular
by a [backfill](backfill.md) job via its `config-id`.

Earlier PAS versions allowed only one configuration per message type.

## Determining the data to archive

There are two mechanisms, chosen per configuration. A condition returning `false`, or a provider
returning `null`, skips that configuration without archiving an artifact.

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
| `oauthClientId`                | no       | Spring OAuth2 client registration name used to obtain a token; if omitted the API is called without authentication |

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

`oauthClientId` selects the key below `spring.security.oauth2.client.registration`, not the
identity provider's client ID stored in that registration's `client-id` property. For example,
`oauthClientId: archive-source` selects `spring.security.oauth2.client.registration.archive-source`.

The source service must implement the [standardized archive-data REST interface](archive-data-rest-interface.md).
For archiving PCS snapshots, the [PCS snapshot documentation](https://jeap-admin-ch.github.io/docs/building-blocks/reusable-microservices/jeap-process-context-service/process-templates#rest-interface-for-process-snapshots)
defines the endpoint, version parameter, authorization and schema prerequisites.

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
package ch.admin.bit.jeap.jme.processarchive.service.provider;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageArchiveDataProvider;
import ch.admin.bit.jeap.processarchive.test.DecreeReference;
import ch.admin.bit.jeap.processarchive.test.decree.v3.Decree;
import ch.admin.bit.jeap.processarchive.web.AvroBinarySerializer;
import ch.admin.bit.jme.decree.JmeDecreeCreatedEvent;
import ch.admin.bit.jme.decree.JmeDecreeCreatedEventPayload;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;

public class DecreeCreatedDataProvider implements MessageArchiveDataProvider<JmeDecreeCreatedEvent> {

    private final AvroBinarySerializer avroBinarySerializer;

    public DecreeCreatedDataProvider() {
        this.avroBinarySerializer = new AvroBinarySerializer();
    }

    @Override
    public ArchiveData getArchiveData(JmeDecreeCreatedEvent jmeDecreeCreatedEvent) {
        return ArchiveData.builder()
                .referenceId(jmeDecreeCreatedEvent.getReferences().getNewDecree().getId())
                .system("JME")
                .schema("Decree")
                .schemaVersion(3)
                .contentType("avro/binary")
                .payload(createPayload(jmeDecreeCreatedEvent.getPayload(),
                        jmeDecreeCreatedEvent.getReferences().getNewDecree().getId()))
                .build();
    }

    @SneakyThrows
    private byte[] createPayload(JmeDecreeCreatedEventPayload payload, String referenceId) {
        DecreeReference example = DecreeReference.newBuilder()
                .setId(referenceId)
                .setType("Example")
                .build();
        Decree decree = Decree.newBuilder()
                .setTitle(payload.getTitle())
                .setPayload(payload.getSomeDecreeData())
                .setCreatedAt(payload.getCreatedAt())
                .setDecreeReference(example)
                .build();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            avroBinarySerializer.serialize(decree, outputStream);
            return outputStream.toByteArray();
        }
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
    },
    {
      "id": "dossier",
      "messageName": "JmeDossierClosedEvent",
      "topicName": "jme-process-archive-dossierclosed",
      "archiveDataReferenceProvider": "ch.admin.bit.jeap.jme.processarchive.service.provider.DossierClosedArchiveDataReferenceProvider",
      "uri": "${dossier.archive.uri}",
      "oauthClientId": "jme-process-archive-resource-service"
    },
    {
      "id": "dossier-receipt",
      "messageName": "JmeDossierClosedEvent",
      "topicName": "jme-process-archive-dossierclosed",
      "messageArchiveDataProvider": "ch.admin.bit.jeap.jme.processarchive.service.provider.DossierClosedReceiptDataProvider"
    }
  ]
}
```

The last two entries show [multiple artifacts per message](#multiple-artifacts-per-message):
`JmeDossierClosedEvent` is configured twice — once fetching the dossier from a remote service and once
archiving a receipt built from the message payload. Both configurations use the same `topicName` and
each declares a unique `id`.

## Process correlation

By default, the artifact's process ID comes from the incoming message's `processId`. Configure a
`MessageCorrelationProvider` if the source of the process ID differs, for example a business identifier
in the payload. This is separate from the archive-data reference used to fetch or name the artifact.

Add the provider to the corresponding entry in `processarchive/messages.json`:

```json
"correlationProvider": "ch.admin.bit.jeap.jme.processarchive.service.provider.CorrelationProvider"
```

```java
package ch.admin.bit.jeap.jme.processarchive.service.provider;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageCorrelationProvider;

// MyEvent represents the application's generated message type.
public class CorrelationProvider implements MessageCorrelationProvider<MyEvent> {
    @Override
    public String getOriginProcessId(MyEvent event) {
        return event.getPayload().getOtherProcessId();
    }
}
```

## Plugin interfaces

The relevant SPI interfaces live in `jeap-process-archive-plugin-api`. Referenced provider classes are
instantiated by the PAS from their fully-qualified name in `messages.json`. These classes need an
accessible no-argument constructor and are not obtained from the Spring bean factory; Spring dependency
injection is therefore not performed on them. Listeners, hash providers, archive-type providers and
object-storage strategies are picked up as Spring beans.

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
