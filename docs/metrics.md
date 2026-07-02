# Metrics

The PAS exposes Prometheus/Micrometer metrics with the prefix `jeap_pas_*`. All are timers and are
published with `_count` / `_max` / `_sum`; the S3 timers additionally publish the 0.5 / 0.8 / 0.95 / 0.99
quantiles. All standard Kafka metrics are available as well.

| Name                                                            | Type    | Description                                               |
|-----------------------------------------------------------------|---------|-----------------------------------------------------------|
| `jeap_pas_archive_message_seconds`                              | Timer   | Processing time of a message from fetch to commit         |
| `jeap_pas_archive_message_seconds_count`                        | Counter | Number of archived artifacts over time                    |
| `jeap_pas_remote_archive_data_factory_seconds`                  | Timer   | Source service response time per message type             |
| `jeap_pas_put_object_with_lifecycle_policy_seconds`             | Timer   | Time to store archive data                                |
| `jeap_pas_get_object_properties_seconds`                        | Timer   | Time to fetch object metadata                             |
| `jeap_pas_put_object_seconds`                                   | Timer   | Time to store a schema                                    |
| `jeap_pas_encrypt_payload_seconds`                              | Timer   | Time for [encryption](encryption.md) (jEAP Crypto)        |
| `jeap_pas_s3_client_put_object_seconds`                         | Timer   | S3 response time for storing an object                    |
| `jeap_pas_s3_client_does_object_exist_seconds`                  | Timer   | S3 response time for existence checks                     |
| `jeap_pas_s3_client_head_object_seconds`                        | Timer   | S3 response time for object metadata (HEAD)               |
| `jeap_pas_s3_client_get_bucket_lifecycle_configuration_seconds` | Timer   | S3 response time for fetching the lifecycle configuration |
| `jeap_pas_s3_client_put_bucket_lifecycle_configuration_seconds` | Timer   | S3 response time for storing the lifecycle configuration  |

To access the metrics endpoint, configure the Prometheus password:

```yaml
jeap:
  monitor:
    prometheus:
      password: "..."
```

## Related

- [Configuration reference](configuration.md)
- [Archived artifacts & events](archived-artifacts.md)
