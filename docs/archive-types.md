# Archive types

An *archive type* describes a kind of archived data. Technically the PAS can archive any byte stream,
but archive types carry the metadata the PAS needs — notably the reference-id type and the
[expiration period](data-expiration-and-locking.md) — and, for Avro, enable schema validation.

An archive type is identified by the coordinates **(system, archive type name, version)**. Archive-type
names are only unique within a system: two systems may both define an archive type named `Decree`.

There are two ways to make archive types known to a PAS instance.

## Avro-defined archive types (recommended)

Avro archive types are defined in an [Archive Type Registry](archive-type-registry.md), which generates
immutable Java binding artifacts. A PAS instance:

1. Depends on the generated binding artifact for every archive-type version it must handle:

   ```xml
   <dependency>
       <groupId>ch.admin.bit.jme.archivetype.jme</groupId>
       <artifactId>decree-v3</artifactId>
       <version>3</version>
   </dependency>
   ```

   > The PAS must know the schema of every archive-type version it archives. After defining a new
   > version in the registry and merging it, add the dependency to the PAS **before** data is archived
   > with that schema.

2. Registers those versions at runtime via an `ArchiveTypeProvider` bean:

   ```java
   @Component
   public class JmeArchiveTypeProvider implements ArchiveTypeProvider {
       @Override
       public List<Class<? extends SpecificRecordBase>> getArchiveTypeVersions() {
           return List.of(
               ch.admin.bit.jeap.processarchive.test.decree.v1.Decree.class,
               ch.admin.bit.jeap.processarchive.test.decree.v2.Decree.class,
               DecreeDocument.class,
               Diagram.class);
       }
   }
   ```

   Register the bean through
   `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

### Schema validation

When the content type of the data is `avro/binary`, the PAS validates the data against the Avro schema
selected by the coordinates (system / archive type / version). This ensures the data can be read back
later. See [Archive Type Registry](archive-type-registry.md) for how schemas are defined and evolved.

## Config-defined archive types

Archive types that are **not** Avro-based are not schema-validated and must be listed in configuration
(PAS 11+):

```yaml
jeap:
  processarchive:
    registry:
      types:
        - archive-type: JsonExample   # (archive-type, system, version) is the key of an archive-type version
          system: JME
          version: 1
          expiration-days: 365        # retention, enforced via S3 lifecycle policy
          reference-id-type: ch.admin.bit.jeap.jme.JsonExampleReferenceId
```

`reference-id-type` can be chosen freely; it is used when building the
[`SharedArchivedArtifactVersionCreatedEvent`](archived-artifacts.md).

## Related

- [Archive Type Registry](archive-type-registry.md)
- [Consuming messages](consuming-messages.md)
- [Data expiration & locking](data-expiration-and-locking.md)
- [Encryption](encryption.md)
