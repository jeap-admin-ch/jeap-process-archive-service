# Configuration reference

PAS-specific properties live under `jeap.processarchive.*`. The PAS is also a jEAP Messaging service, so
Kafka is configured under `jeap.messaging.kafka.*` and `spring.kafka.*` — see the
[jEAP Messaging](https://jeap-admin-ch.github.io/docs/building-blocks/libraries/jeap-messaging/) documentation. Relaxed binding
applies, so `object-lock-enabled` and `objectLockEnabled` are equivalent.

## Archive configuration

| Property                                     | Description                                                                              | Default                                   |
|----------------------------------------------|------------------------------------------------------------------------------------------|-------------------------------------------|
| `jeap.processarchive.configuration.location` | Location of the archive configuration file                                               | `classpath:/processarchive/messages.json` |
| `jeap.processarchive.registry.types`         | Config-defined ([non-Avro](archive-types.md#config-defined-archive-types)) archive types |                                           |

The message-triggered archiving is declared in [`messages.json`](consuming-messages.md), where each
configuration entry may carry an `id` (mandatory and unique when a message has multiple configurations).

## Archived artifact event

See [Archived artifacts & events](archived-artifacts.md).

The following values must be non-empty when publication is enabled. Setting `enabled=false`
disables the event and its property validation. The publisher identity defaults to the jEAP Messaging
identity, so it normally does not need to be configured a second time.

| Property | Required when enabled | Description | Default |
| --- | --- | --- | --- |
| `jeap.processarchive.archivedartifact.event-topic` | yes | Topic for `SharedArchivedArtifactVersionCreatedEvent` | None |
| `jeap.processarchive.archivedartifact.system-id` | yes | `systemId` of the event (freely chosen) | None |
| `jeap.processarchive.archivedartifact.system-name` | yes | Publisher system name | `jeap.messaging.kafka.system-name` |
| `jeap.processarchive.archivedartifact.service-name` | yes | Publisher service name | `jeap.messaging.kafka.service-name` |
| `jeap.processarchive.archivedartifact.enabled` | no | Publish the event | `true` |

## Object storage

See [Object storage](object-storage.md) and [Data expiration & locking](data-expiration-and-locking.md).

| Property                                                     | Description                                  | Default      |
|--------------------------------------------------------------|----------------------------------------------|--------------|
| `jeap.processarchive.objectstorage.connection.access-url`    | S3 URL                                       |              |
| `jeap.processarchive.objectstorage.connection.region`        | S3 / AWS region                              | `AWS_GLOBAL` |
| `jeap.processarchive.objectstorage.connection.access-key`    | S3 access key                                |              |
| `jeap.processarchive.objectstorage.connection.secret-key`    | S3 secret key                                |              |
| `jeap.processarchive.objectstorage.storage.bucket`           | Default-strategy target bucket               |              |
| `jeap.processarchive.objectstorage.storage.prefix-mode`      | Key prefix mode: `NONE`/`DAY`/`MONTH`/`YEAR` | `DAY`        |
| `jeap.processarchive.objectstorage.object-lock-enabled`      | Enable object locking                        | `true`       |
| `jeap.processarchive.objectstorage.object-lock-mode`         | `COMPLIANCE` or `GOVERNANCE`                 | `COMPLIANCE` |
| `jeap.processarchive.objectstorage.schema-overwrite-allowed` | Allow overwriting immutable Avro schemas on S3         | `false`      |

## Remote data fetch

| Property                           | Description                                                                     | Default |
|------------------------------------|---------------------------------------------------------------------------------|---------|
| `jeap.processarchive.http.timeout` | Read timeout for the OAuth-authenticated [remote-data](archive-data-rest-interface.md) client | `5s`    |

The current implementation applies this property as a read timeout when building the OAuth client.
It is not a total end-to-end deadline and does not configure the unauthenticated client; that client
uses the supplied `RestClient.Builder` settings.

If a source service is slow, raise this — but not too high, or a temporarily unavailable service could
delay archiving for other services while the PAS waits for timeouts.

## Kafka polling

As for any jEAP Messaging consumer, polling must be tuned to the expected processing time per message:

| Property                                                | Description                                                          | PAS default |
|---------------------------------------------------------|----------------------------------------------------------------------|-------------|
| `spring.kafka.consumer.properties.max.poll.records`     | Max records fetched per poll                                         | `10`        |
| `spring.kafka.consumer.properties.max.poll.interval.ms` | Max time between polls before the broker considers the consumer dead | `100000`    |

Budget `max.poll.interval.ms` for the total time needed to process a poll's records. For a message
with several archive configurations, include every remote fetch and artifact write, plus schema
validation, encryption and listener execution. `max.poll.records × jeap.processarchive.http.timeout`
alone is not a sufficient bound: it assumes only one fetch per record, omits other processing, and
the read timeout is not a total request deadline. Include connection and authentication delays and
use the effective client timeout settings when choosing the polling interval.

## Backfill

See [Backfill jobs](backfill.md) for the full list (`jeap.processarchive.backfill.*`).

## Avro class whitelist

The PAS uses the whitelist installed by jEAP Messaging. For defaults, trusted packages/classes and
startup initialization, see the [Avro class whitelist](https://jeap-admin-ch.github.io/docs/building-blocks/libraries/jeap-messaging/avro-class-security).

Include the generated archive types as well as the message types when configuring trusted packages.
The same requirement applies when [reading archived data](reading-archived-data.md). The PAS does
not install a second whitelist.

## Metrics

See [Metrics](metrics.md); set `jeap.monitor.prometheus.password` to access the endpoint.

## Related

- [Consuming messages](consuming-messages.md)
- [Object storage](object-storage.md)
- [Archived artifacts & events](archived-artifacts.md)
- [Backfill jobs](backfill.md)
