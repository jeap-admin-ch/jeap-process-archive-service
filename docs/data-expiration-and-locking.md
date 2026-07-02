# Data expiration & locking

Every [archive type](archive-types.md) defines a retention period in days (`expirationDays` in the
descriptor, or `expiration-days` for config-defined types). During this period archived data is
protected from manipulation (overwrite, delete) in the object store. Deleting data after the period is
ultimately the responsibility of the archiving system — **the PAS never deletes data**. To support this,
the PAS configures S3 lifecycle rules and object locks.

## Lifecycle rules

Before storing data, the PAS ensures S3 lifecycle configuration rules exist on the bucket. For a
versioned object these rules:

- transition the current version to *noncurrent* after `expirationDays` days (a delete marker becomes
  the current version);
- permanently delete a *noncurrent* version after a further `expirationDays` days.

So physical deletion happens after `2 × expirationDays` days.

The PAS manages the rules additively: it creates lifecycle rules for all archive types it knows, and if
a rule is missing when storing data it adds the missing rules for all types. Rules are only added, never
read-modify-written, to avoid concurrency issues between multiple PAS instances and to avoid deleting
rules not managed by the PAS.

You can inspect an object's expiry with:

```bash
aws s3api head-object --bucket <bucket> --key <key>   # see the "Expiration" field
```

## Object lock

In addition to the lifecycle rules, the PAS places an object lock on the stored data that protects it
from manipulation — and also from the automatic lifecycle deletion — for the retention period. If a
lifecycle deletion would fall before the lock expires, it is deferred until the lock expires.

```bash
aws s3api get-object-retention --bucket <bucket> --key <key>   # see Mode and RetainUntilDate
```

## Configuration

| Property                                                     | Description                                                                         | Default      |
|--------------------------------------------------------------|-------------------------------------------------------------------------------------|--------------|
| `jeap.processarchive.objectstorage.object-lock-enabled`      | Enable object locking                                                               | `true`       |
| `jeap.processarchive.objectstorage.object-lock-mode`         | `COMPLIANCE` or `GOVERNANCE`                                                        | `COMPLIANCE` |
| `jeap.processarchive.objectstorage.schema-overwrite-allowed` | Allow overwriting Avro schemas on S3 (normally not desired — schemas are immutable) | `false`      |

With `COMPLIANCE` objects may be undeletable for a long time, which is often undesirable on non-prod
environments. A common per-environment setup is:

| Environment | Object lock                                                   |
|-------------|---------------------------------------------------------------|
| DEV / REF   | `object-lock-enabled = false`                                 |
| ABN         | `object-lock-enabled = true`, `object-lock-mode = GOVERNANCE` |
| PROD        | `object-lock-enabled = true`, `object-lock-mode = COMPLIANCE` |

## Related

- [Object storage](object-storage.md)
- [Archive types](archive-types.md)
- [Configuration reference](configuration.md)
