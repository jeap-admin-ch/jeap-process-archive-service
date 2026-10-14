# Object storage

The PAS stores archived data in an S3-compatible object store. This page covers the connection, bucket
requirements, and how bucket/key are chosen. Retention and immutability are covered in
[Data expiration & locking](data-expiration-and-locking.md).

## Connection

Connection properties and defaults are listed in [Configuration](configuration.md#object-storage).

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

### Inspecting bucket versioning

For an S3-compatible endpoint, add `--endpoint-url` to the commands as needed. Before using the bucket,
inspect its versioning and the stored versions:

```bash
aws s3api get-bucket-versioning --bucket <bucket-name>
aws s3api list-object-versions --bucket <bucket-name> --prefix <object-key>
```

Reading a specific version is useful after a delete marker hides the current object. See
[Lifecycle rules](data-expiration-and-locking.md#lifecycle-rules) for expiry and retention checks.

### Buckets without object lock

Some storage backends cannot enable object lock on an existing bucket. For existing PAS instances set up
on buckets without object lock, set `jeap.processarchive.objectstorage.object-lock-enabled=false` as a
transition measure; migrate the data to object-lock buckets afterwards.

With DELL ECS, ADO (Access During Outage) must be **disabled** when object lock is used, to avoid data
loss during an outage — the audit trail must not be altered, and temporary unavailability is acceptable
because access to archived data is not time-critical and the error handling tolerates temporary outages.

## Object storage strategy

Bucket and key for an artifact are chosen by the configured `ObjectStorageStrategy`. The default
strategy honors non-blank `Archive-Storage-Bucket`/`Archive-Storage-Prefix`
[headers](archive-data-rest-interface.md) in preference to its configured bucket/date prefix.
A custom strategy receives these values through `ArchiveData` and decides how to apply them.

### Default strategy

Without a custom strategy, the default bucket comes from configuration. The object name is the result
of `HashProvider.hashStorageObjectId(referenceId, referenceIdType)`, where the reference-ID type comes
from the archive type. The complete key is the prefix followed by that name. The date prefix uses the
date of archiving. Set the bucket and prefix mode in [Configuration](configuration.md#object-storage).
Use the bucket, key and S3 version ID reported in the archived artifact to retrieve a stored version;
do not assume the key equals the business reference ID.

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

The object-storage adapter requires a `HashProvider` Spring bean supplied by the PAS instance or one
of its dependencies. It must implement both methods:

| Method | Purpose |
| --- | --- |
| `hashPayload(byte[])` | Hashes archive payloads and schemas for object metadata. |
| `hashStorageObjectId(String referenceId, String referenceIdType)` | Derives the object name used by the default storage strategy. |

The interface's default `hashReferenceId` method delegates to `hashStorageObjectId`. The PAS does not
provide a default hashing implementation. Changing the implementation can change storage-object names;
coordinate such changes with the archive catalog and readers.

## Schema storage on the bucket

For Avro archive types, the schema is stored next to the data on the bucket as a self-contained Avro
protocol (`.avpr`), so the archived data can be read without external schema resolution. The schema
contains all required type definitions without external imports. By convention, one type has the
archive type's name. The schema is stored once per archive-type version at:

```text
archive-data-schema/<system>_<archivetype>_<version>.avpr      e.g. archive-data-schema/JME_Decree_1.avpr
```

The key has no leading slash. The location is recorded on the archived object as the `schema-file-key` metadata entry. Set
`jeap.processarchive.objectstorage.schema-overwrite-allowed=false` to never overwrite an existing schema
(schemas of a published archive-type version are immutable).

## Related

- [Data expiration & locking](data-expiration-and-locking.md)
- [Archive types](archive-types.md)
- [Reading archived data](reading-archived-data.md)
- [Configuration reference](configuration.md)
