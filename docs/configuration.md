# Configuration reference

PAS-specific properties live under `jeap.processarchive.*`. The PAS is also a jEAP Messaging service, so
Kafka is configured under `jeap.messaging.kafka.*` and `spring.kafka.*` — see the
[jEAP Messaging](https://github.com/jeap-admin-ch/jeap-messaging) documentation. Relaxed binding
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

| Property                                           | Required | Description                                               | Default |
|----------------------------------------------------|----------|-----------------------------------------------------------|---------|
| `jeap.processarchive.archivedartifact.event-topic` | yes      | Topic for the `SharedArchivedArtifactVersionCreatedEvent` |         |
| `jeap.processarchive.archivedartifact.system-id`   | yes      | `systemId` of the event                                   |         |
| `jeap.processarchive.archivedartifact.system-name` | yes      | System name used in the event                             |         |
| `jeap.processarchive.archivedartifact.enabled`     | no       | Publish the event                                         | `true`  |

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
| `jeap.processarchive.objectstorage.schema-overwrite-allowed` | Allow overwriting Avro schemas on S3         | `false`      |

## Remote data fetch

| Property                           | Description                                                                     | Default |
|------------------------------------|---------------------------------------------------------------------------------|---------|
| `jeap.processarchive.http.timeout` | Max time for one successful [remote data](archive-data-rest-interface.md) fetch | `5s`    |

If a source service is slow, raise this — but not too high, or a temporarily unavailable service could
delay archiving for other services while the PAS waits for timeouts.

## Kafka polling

As for any jEAP Messaging consumer, polling must be tuned to the expected processing time per message:

| Property                                                | Description                                                          | PAS default |
|---------------------------------------------------------|----------------------------------------------------------------------|-------------|
| `spring.kafka.consumer.properties.max.poll.records`     | Max records fetched per poll                                         | `10`        |
| `spring.kafka.consumer.properties.max.poll.interval.ms` | Max time between polls before the broker considers the consumer dead | `100000`    |

The main source of delay is fetching data over REST. Configure `max.poll.interval.ms` **greater than**
`max.poll.records × jeap.processarchive.http.timeout`, plus headroom for the artifact listener and the
object store — otherwise an unavailable source service could stall the PAS.

## Backfill

See [Backfill jobs](backfill.md) for the full list (`jeap.processarchive.backfill.*`).

## Metrics

See [Metrics](metrics.md); set `jeap.monitor.prometheus.password` to access the endpoint.

## Related

- [Consuming messages](consuming-messages.md)
- [Object storage](object-storage.md)
- [Archived artifacts & events](archived-artifacts.md)
- [Backfill jobs](backfill.md)
