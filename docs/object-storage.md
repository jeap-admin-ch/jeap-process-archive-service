# Object storage

The PAS stores archived data in an S3-compatible object store. This page covers the connection, bucket
requirements, and how bucket/key are chosen. Retention and immutability are covered in
[Data expiration & locking](data-expiration-and-locking.md).

## Connection

Configure the connection under `jeap.processarchive.objectstorage.connection`:

| Property     | Description     | Default      |
|--------------|-----------------|--------------|
| `access-url` | S3 URL          |              |
| `region`     | S3 / AWS region | `AWS_GLOBAL` |
| `access-key` | S3 access key   |              |
| `secret-key` | S3 secret key   |              |

On AWS, when the PAS itself is deployed in AWS, `access-url`, `access-key` and `secret-key` can be
omitted — credentials are taken from the context (IAM role). Set `region` to the project-specific region.

## Bucket requirements

> Follow the [S3 bucket naming rules](https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html).

The target buckets must exist before archiving — the PAS does not create them (enabling versioning can
take a while). Buckets must have **versioning** enabled, and for production, **object lock** enabled so
archived data cannot be overwritten and can only be deleted after a retention period.

```bash
# create a bucket with object lock (implies versioning) on AWS S3
aws s3api create-bucket --bucket <bucket-name> --object-lock-enabled-for-bucket
```

### Buckets without object lock

Some storage backends cannot enable object lock on an existing bucket. For existing PAS instances set up
on buckets without object lock, set `jeap.processarchive.objectstorage.object-lock-enabled=false` as a
transition measure; migrate the data to object-lock buckets afterwards.

With DELL ECS, ADO (Access During Outage) must be **disabled** when object lock is used, to avoid data
loss during an outage — the audit trail must not be altered, and temporary unavailability is acceptable
because access to archived data is not time-critical and the error handling tolerates temporary outages.

## Object storage strategy

Bucket and key for an artifact are chosen by the configured `ObjectStorageStrategy`. If a source
endpoint sets the `Archive-Storage-Bucket`/`Archive-Storage-Prefix`
[headers](archive-data-rest-interface.md), those values take precedence.

### Default strategy

Without a custom strategy the default one archives everything into a single configurable bucket, using
the reference id as the key, optionally with a date prefix. Configure it under
`jeap.processarchive.objectstorage.storage`:

| Property      | Description                                     | Default |
|---------------|-------------------------------------------------|---------|
| `bucket`      | Target bucket for archived data                 |         |
| `prefix-mode` | Key prefix mode: `NONE`, `DAY`, `MONTH`, `YEAR` | `DAY`   |

| Prefix mode | Example prefix |
|-------------|----------------|
| `NONE`      | (none)         |
| `DAY`       | `20210614/`    |
| `MONTH`     | `202106/`      |
| `YEAR`      | `2021/`        |

### Custom strategy

For more control (e.g. multiple buckets), implement the `ObjectStorageStrategy` plugin interface and
register it as a Spring bean via an auto-configuration that runs before `ObjectStorageConfiguration`:

```java
@AutoConfiguration(before = ObjectStorageConfiguration.class)
public class CustomConfiguration {
    @Bean
    CustomStrategy customStrategy() { return new CustomStrategy(/* ... */); }
}
```

Register it in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

## Hash provider

For every archived record the PAS computes a hash and stores it in the S3 object metadata. The hashing
algorithm is defined by a `HashProvider` plugin implementation registered as a Spring bean.

## Schema storage on the bucket

For Avro archive types, the schema is stored next to the data on the bucket as a self-contained Avro
protocol (`.avpr`), so the archived data can be read without external schema resolution. The schema is
stored once per archive-type version at:

```text
/archive-data-schema/<system>_<archivetype>_<version>.avpr      e.g. /archive-data-schema/JME_Decree_1.avpr
```

The location is recorded on the archived object as the `schema-file-key` metadata entry. Set
`jeap.processarchive.objectstorage.schema-overwrite-allowed=false` to never overwrite an existing schema
(schemas of a published archive-type version are immutable).

## Related

- [Data expiration & locking](data-expiration-and-locking.md)
- [Archive types](archive-types.md)
- [Reading archived data](reading-archived-data.md)
- [Configuration reference](configuration.md)
