# OpenSearch integration

Optionally, archived artifacts can be indexed into OpenSearch so they can be searched. This is provided
by the `jeap-process-archive-adapter-opensearch` module, which exposes a REST API that an index writer
service calls, converting each archived artifact into a search item.

## Enable the REST API

Add the dependency to the PAS instance:

```xml
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-process-archive-adapter-opensearch</artifactId>
</dependency>
```

Define an OAuth client for the index writer service with the required role
(`<system>_@searchitem_#read`) so it may call the API.

## Define index type mappings

For each archive type that should be indexed, add a mapping in
`src/main/resources/processarchive/indextypes.json`:

```json
{
  "indexTypes": [
    {
      "indexType": "JmeDecree",
      "archiveType": "ch.admin.bit.jeap.processarchive.test.decree.v3.Decree",
      "archiveTypeToSearchItemConverter": "ch.admin.bit.jeap.jme.processarchive.service.indextype.DecreeConverter"
    }
  ]
}
```

Implement an `ArchiveTypeToSearchItemConverter` per archive type that maps the archived payload to a
search item:

```java
public class DecreeConverter implements ArchiveTypeToSearchItemConverter<Decree> {
    @Override
    public SearchItemContainer convert(Decree payload, String archiveId, String version, Map<String, String> metadata) {
        // build and return a SearchItemContainer
    }
}
```

Add the corresponding index-type Java binding dependency so the generated classes are available, e.g.:

```xml
<dependency>
    <groupId>ch.admin.bit.jme.indextype.jme</groupId>
    <artifactId>jme-decree-document-v1</artifactId>
    <version>1.0</version>
</dependency>
```

## Related

- [Archived artifacts & events](archived-artifacts.md)
- [Archive types](archive-types.md)
- [Getting started](getting-started.md)
