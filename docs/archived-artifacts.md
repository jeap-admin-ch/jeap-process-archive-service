# Archived artifacts & events

When the PAS has archived a record it accumulates everything about the archiving operation (process
data, metadata, object metadata) into an **archived artifact** and makes it available in two ways: an
in-process listener interface and a published Kafka event.

## The archived artifact

An archived artifact exposes, among others:

| Field                                    | Description                                                                        |
|------------------------------------------|------------------------------------------------------------------------------------|
| Process Id                               | Id of the process in which the data was created/changed/deleted                    |
| Reference Id                             | Id of the archived record (unique together with the version)                       |
| Reference Id Type                        | Type stored in the catalog                                                         |
| Version                                  | Version of the record (may be empty if unversioned)                                |
| Payload / Content Type                   | The archived data (binary) and its content type                                    |
| System / Schema / Schema Version         | The [archive type](archive-types.md) coordinates                                   |
| Storage Object Bucket / Key / Version Id | Where the data was stored in S3 (version id assigned by the store)                 |
| Expiration Days                          | Retention period (see [Data expiration & locking](data-expiration-and-locking.md)) |
| Metadata                                 | Additional application-defined metadata                                            |

## ArtifactArchivedListener

Implement the `ArtifactArchivedListener` plugin interface (as a Spring bean) to receive each archived
artifact — for example to record the S3 coordinates (bucket/key) in an archive catalog so archived
records can be searched and retrieved later.

```java
@Component
public class CatalogArtifactListener implements ArtifactArchivedListener {
    @Override
    public void onArtifactArchived(ArchivedArtifact archivedArtifact) {
        // e.g. register the artifact in a catalog
    }
}
```

## SharedArchivedArtifactVersionCreatedEvent

For every archived artifact the PAS publishes a `SharedArchivedArtifactVersionCreatedEvent`, notifying
consumers that a new artifact version exists in the archive. The event's `variant` is set to
`<System>_<DataSchemaType>`, e.g. `JME_DecreeDocument`. The event definition lives in the jEAP Message
Type Registry.

| Property                                           | Required | Description                                            |
|----------------------------------------------------|----------|--------------------------------------------------------|
| `jeap.processarchive.archivedartifact.event-topic` | yes      | Topic the event is published to                        |
| `jeap.processarchive.archivedartifact.system-id`   | yes      | Value of the event's `systemId` (freely chosen)        |
| `jeap.processarchive.archivedartifact.system-name` | yes      | System name used in the event                          |
| `jeap.processarchive.archivedartifact.enabled`     | no       | Set to `false` to disable publication (default `true`) |

## Idempotence

Each archived artifact gets an idempotence id, and the published event's idempotence id is that value
with a `-event` suffix. The id is derived deterministically so retries are idempotent:

```text
<messageType>_<sha256-hex over (messageIdempotenceId, system, schema, referenceId[, version])>
```

The message type prefix keeps the id attributable in logs; the SHA-256 hash (64 lowercase hex characters)
covers the incoming message's idempotence id and a discriminator (`system`, `schema`, `referenceId`, and
`version` when present) that makes the id unique per artifact, while keeping the id length fixed at the
message type name plus 65 characters. Each field in the hash input is length-prefixed, so field boundaries
are unambiguous and distinct field values cannot collide. This matters when one message produces
[multiple artifacts](consuming-messages.md#multiple-artifacts-per-message): the PAS requires the ids of
all artifacts of a message to be distinct and fails fast otherwise.

Because delivery is at-least-once, a retried message re-archives its artifacts (a new S3 object version)
and re-publishes their events; downstream consumers deduplicate by the event idempotence id.

## Publication feature flag

Publication of an archived artifact can be gated per message with a `featureFlag` in
[`messages.json`](consuming-messages.md), which lets you deploy an archiving configuration without
activating it yet:

```json
{ "messageName": "JmeDecreeCreatedEvent", "topicName": "...", "featureFlag": "FEATURE_DECREE_CREATED" }
```

When the flag is inactive at processing time, the artifact is not announced to the listeners (and no
event is published). Once the flag is activated, new artifacts are announced; artifacts processed while
the flag was inactive are **not** announced retroactively.

Feature-flag state is configured with Togglz:

```yaml
togglz:
  features:
    FEATURE_DECREE_CREATED:
      enabled: true
```

The PAS also exposes the standard feature-flag metric, e.g.
`feature_flag{client="...",name="FEATURE_DECREE_CREATED"} 1.0`.

## Related

- [Consuming messages](consuming-messages.md)
- [Metrics](metrics.md)
- [Reading archived data](reading-archived-data.md)
- [OpenSearch integration](opensearch.md)
