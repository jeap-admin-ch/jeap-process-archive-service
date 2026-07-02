# Archive Type Registry

The PAS supports arbitrary transport formats for archive data, but Avro is recommended (and mandatory
for some programmes such as DaziT). The Archive Type Registry is the design-time schema repository for
Avro [archive types](archive-types.md): a structured Git repository that manages archive-data schemas
and protects them from change once published, so archived data can still be read in the future.

Using Avro brings several benefits:

- Java bindings can be generated from the schemas.
- Schemas are managed in a structured, validated way.
- The PAS can validate data against the schema already when storing it, ensuring it can be read later.

## The registry repository

The Archive Type Registry is:

- a **separate Git repository**, maintained directly by the business teams;
- **immutable once published** — a published archive type version can no longer be changed;
- **validated on every build** by the `jeap-process-archive-type-registry-maven-plugin`, and only
  successfully validated builds may be merged to the trunk branch (`master`).

### Layout

```text
schema/                         JSON schema for the descriptor (IDE validation & completion)
archive-types/
  _common/                      global Avro definitions shared across systems
  <system>/                     archive types of one business system
    _common/                    Avro definitions shared within the system
    <archive-type>/
      <archive-type>.json       descriptor (name, system, versions, ...)
      <archive-type>_v1.avdl    schema of version 1
      <archive-type>_v2.avdl    schema of version 2
```

Archive-type names are only unique within a system, so a schema is addressed by the coordinates
**(system, archive-type name, version)**.

## Descriptor

The descriptor defines the type name, system, reference-id type, description, documentation link, an
optional [encryption](encryption.md) key reference, and the schema versions:

```json
{
  "archiveType": "Decree",
  "system": "JME",
  "referenceIdType": "ch.admin.bit.jeap.audit.type.JmeDecreeArchive",
  "description": "archive type example for a decree",
  "documentationUrl": "https://foo/bar",
  "expirationDays": 90,
  "encryptionKey": { "keyId": "my-archive-data-key" },
  "versions": [
    { "version": 1, "schema": "Decree_v1.avdl" },
    { "version": 2, "schema": "Decree_v2.avdl", "compatibilityMode": "BACKWARD" },
    { "version": 3, "schema": "Decree_v3.avdl", "compatibilityMode": "BACKWARD", "compatibleVersion": 1 }
  ]
}
```

`compatibilityMode` is one of `BACKWARD`, `FORWARD`, `FULL` or `NONE`. By convention the root schema of
a version is an Avro protocol whose record name equals the archive-type name, and each version uses its
own namespace so generated classes for different versions do not collide:

```text
@namespace("ch.admin.bit.jeap.processarchive.test.decree.v3")
protocol DecreeProtocol {
    import idl "ch.admin.bit.jeap.processarchive.test.DecreeReference.avdl";
    record Decree {
        ch.admin.bit.jeap.processarchive.test.DecreeReference decreeReference;
        timestamp_ms createdAt;
        string title;
        string payload;
    }
}
```

## Validation

The `jeap-process-archive-type-registry-maven-plugin` validates, among other things, that:

- schemas merged to the trunk branch have not been changed;
- the Avro protocol contains a record named like the archive type;
- referenced schemas parse correctly (including imports);
- the registry layout matches the expected structure;
- schemas are named `<archive-type>_v<version>.avdl`;
- versions are compatible according to their `compatibilityMode`.

```xml
<plugin>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-process-archive-type-registry-maven-plugin</artifactId>
    <configuration>
        <gitUrl>https://.../dazit-archive-type-registry.git</gitUrl>
    </configuration>
    <executions>
        <execution><goals><goal>registry</goal></goals></execution>
    </executions>
</plugin>
```

## Publishing Java bindings

The `jeap-process-archive-avro-maven-plugin` generates Java bindings from the Avro schemas and can
deploy them to a Maven repository, so PAS instances and source services can consume them as normal
dependencies. It offers the `compile-archive-types` and `deploy-archive-type-artifacts` goals. See the
`jme-archive-type-registry` project for a complete example.

## Related

- [Archive types](archive-types.md)
- [Archive-data REST interface](archive-data-rest-interface.md)
- [Encryption](encryption.md)
