# Reading archived data

The PAS itself only writes artifacts. To read archived artifacts back from object storage, use the
separate **jeap-process-archive-reader** library:
[github.com/jeap-admin-ch/jeap-process-archive-reader](https://github.com/jeap-admin-ch/jeap-process-archive-reader).

It is a small Spring Boot library that, given a bucket and object key, fetches the binary Avro object,
reads the writer schema stored alongside it, and deserializes it into the generated reader type
(schema-on-read). It provides:

- a `ProcessArchiveReader` bean returning archived objects as strongly-typed, Avro-generated Java objects;
- schema-on-read: the writer schema is fetched from the archive and resolved against the reader schema;
- retrieval of the current object version or a specific S3 object version;
- client-side decryption of [encrypted](encryption.md) artifacts via jEAP Crypto;
- auto-configuration that reuses an existing `S3Client` bean or builds one from connection properties.

See that repository's documentation (`docs/`) for getting started, how it works, configuration
(`jeap.process-archive.reader.*`) and reading encrypted artifacts.

## Related

- [Object storage](object-storage.md)
- [Archive types](archive-types.md)
- [Encryption](encryption.md)
