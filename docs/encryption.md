# Encryption

The PAS can encrypt archived payloads before storing them in S3, and it can consume encrypted Kafka
records. Both use the [jEAP Crypto](https://github.com/jeap-admin-ch/jeap-crypto) library and a key
management system (HashiCorp Vault, AWS KMS, …).

## Encrypting data at rest

Encryption is enabled per [archive type](archive-types.md) by referencing an encryption key in the
descriptor. If no key is defined, the data is stored unencrypted.

```json
{
  "archiveType": "Decree",
  "encryptionKey": { "keyId": "my-archive-data-key" }
}
```

`keyId` is the id of a key in the PAS's jEAP Crypto configuration, which in turn points to a key in the
key management system. Two prerequisites must be met:

1. A jEAP Crypto starter is on the classpath, for example AWS KMS:

   ```xml
   <dependency>
       <groupId>ch.admin.bit.jeap</groupId>
       <artifactId>jeap-crypto-aws-kms-starter</artifactId>
   </dependency>
   ```

2. jEAP Crypto is configured and can access the key:

   ```yaml
   jeap:
     crypto:
       awskms:
         keys:
           my-archive-data-key:
             key-arn: "${test-key-arn}"
   ```

At startup the PAS validates each configured encryption key by performing a test encryption, so
misconfiguration fails fast. If no data needs encryption, no key management system is required.

### Legacy encryption definition (Vault only)

Older descriptors reference a Vault transit key directly. This cannot be combined with `encryptionKey`:

```json
{
  "archiveType": "Decree",
  "encryption": { "secretEnginePath": "transit/jme", "keyName": "jme-process-archive-example-s3-key" }
}
```

## Consuming encrypted Kafka records

The PAS can consume Kafka records encrypted with jeap-messaging / jeap-crypto like any other jEAP
Messaging consumer — nothing PAS-specific is required. See the jEAP Messaging and jEAP Crypto
documentation for the configuration and dependencies (typically only the Vault URL and system name).

## Related

- [Archive types](archive-types.md)
- [Object storage](object-storage.md)
- [Getting started](getting-started.md)
