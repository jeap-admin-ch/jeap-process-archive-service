# Backfill jobs

Backfill lets users create archived artifacts for data that already exists in a source system but was not archived by the normal event-driven PAS flow. A backfill job contains one or more archive data references. For every reference, PAS publishes a `CreateArtifactCommand`, reads the referenced archive data from the configured source endpoint, stores it in the archive object store, and emits the regular archived artifact event.

Backfill is intended for controlled operational use, for example after enabling PAS for an existing data set or after recovering from an incident where artifacts were not archived.

## Concept

The normal PAS flow archives artifacts when configured domain events are consumed. Backfill starts from explicit archive data references instead:

1. A user submits a YAML backfill job to PAS.
2. PAS validates that the requested message matches a configured remote-data archive type (selected with `config-id`
   when the message has several).
3. PAS persists the job and one task per archive data reference in the database.
4. PAS publishes one `CreateArtifactCommand` per task to the configured Kafka topic.
5. The backfill command consumer reads the command, loads the archive data from the source service using the configured remote data reader endpoint, validates the archive data schema, stores the data, and publishes the archived artifact event.
6. PAS updates each task to `succeeded` or `failed`. When no task remains open, the job becomes `completed` with result `succeeded`, `partially-succeeded`, or `failed`.

Only archive configurations that use remote data are supported. A backfill job cannot be submitted for a message type where the archive data is derived directly from the original event payload, because the original event payload is not part of the backfill request.

## Using jeap-cli

Backfill can be triggered through the `jeap-cli`. The CLI prepares and sends the backfill request to the PAS REST API, so users usually do not need to call the endpoints manually. Use the direct REST API described below when troubleshooting, automating without the CLI, or validating what the CLI sends.

## Enabling backfill

Backfill is disabled by default and must be enabled explicitly in the PAS instance:

```yaml
jeap:
  processarchive:
    backfill:
      enabled: true
      topic: jeap-process-archive-createartifact
      system-name: my-system
      service-name: my-pas-service
```

Properties:

| Property | Required | Default | Description |
| --- | --- | --- | --- |
| `jeap.processarchive.backfill.enabled` | Yes | `false` | Enables the REST endpoints, database adapter, command publisher, and command consumer. |
| `jeap.processarchive.backfill.topic` | Yes when enabled | none | Kafka topic used for `CreateArtifactCommand` messages. The same PAS instance publishes to and consumes from this topic. |
| `jeap.processarchive.backfill.system-name` | Yes when enabled | `${jeap.messaging.kafka.system-name}` | System name used in the command metadata. |
| `jeap.processarchive.backfill.service-name` | Yes when enabled | `${jeap.messaging.kafka.service-name}` | Service name used in the command metadata. |

Add the database adapter dependency to the PAS application's `pom.xml` so that backfill jobs and tasks can be persisted:

```xml
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-process-archive-adapter-db</artifactId>
</dependency>
```

When using the `jeap-process-archive-service-parent`, the version is managed by dependency management. Otherwise, set the dependency version to the same version as the other `jeap-process-archive-*` modules used by the PAS instance.

The PAS instance must also provide the usual dependencies used by archiving:

- A database and the `jeap-process-archive-adapter-db` module on the classpath, because jobs and tasks are persisted in `backfill_job` and `backfill_task`.
- Kafka and the `jeap-process-archive-adapter-kafka` module, because each task is processed through a `CreateArtifactCommand`.
- Object storage, schema validation, and archived artifact event publishing, as for normal PAS processing.
- A matching messaging contract for `CreateArtifactCommand` on the configured backfill topic.

## Archive type configuration requirements

The submitted `message` must match a configured PAS archive type. The archive type must be a remote-data configuration
with a data reader endpoint, for example:

```yaml
messageType: JmeDecreeDocumentCreatedEvent
topic: jme-process-archive-decreedocumentcreated
uri: https://source-service.example/api/archive-data/{id}
oauthClientId: source-service-client
```

During processing, PAS calls the configured remote archive data provider with the `id` from the submitted reference. The source service must return archive data in the format expected by PAS, including the archive data metadata needed for schema validation.

If a source service requires a version, configure a versioned endpoint such as `https://source-service.example/api/archive-data/{id}?version={version}` and include `version` for every submitted archive data reference. Versionless references require a URI template without `{version}`.

### Multiple configurations per message

A message may have [multiple archive configurations](consuming-messages.md#multiple-artifacts-per-message). When more
than one **remote-data** configuration is registered for the submitted `message`, the request must select one with
`config-id` (the configuration's `id` from `messages.json`); otherwise the request is rejected as ambiguous (
`400 Bad Request`). When only one remote-data configuration exists, `config-id` is optional. The selected `config-id` is
stored on the job, echoed in the report, and used when the `CreateArtifactCommand` is processed.

## REST API

The backfill REST API uses YAML. Job submission accepts `application/yaml` and `application/x-yaml`; report retrieval produces `application/yaml`. Both endpoints are only available when `jeap.processarchive.backfill.enabled=true`.

### Submit a job

```http
PUT /api/jobs/{jobId}
Content-Type: application/yaml
```

`jobId` is a UUID chosen by the caller. Reusing the same `jobId` with identical content is idempotent and returns `200 OK`. Reusing the same `jobId` with different content returns `409 Conflict`.

Required role: `backfilljob:write`.

Request body for an unversioned data reader endpoint such as `https://source-service.example/api/archive-data/{id}`:

```yaml
message: JmeDecreeDocumentCreatedEvent
archiveDataReferences:
  - id: DOC-2024-001
  - id: DOC-2024-002
```

Request body for a versioned data reader endpoint such as `https://source-service.example/api/archive-data/{id}?version={version}`:

```yaml
message: JmeDecreeDocumentCreatedEvent
archiveDataReferences:
  - id: DOC-2024-001
    version: 1
  - id: DOC-2024-002
    version: 1
```

Use the unversioned shape when the configured endpoint does not contain `{version}`. Use the versioned shape when the endpoint contains `{version}`; in that case every reference must include `version`.

Request body selecting a specific configuration when the message has multiple remote-data configurations:

```yaml
message: JmeDecreeDocumentCreatedEvent
config-id: decree-document
archiveDataReferences:
  - id: DOC-2024-001
    version: 1
```

Fields:

| Field                             | Required    | Description                                                                                                                               |
|-----------------------------------|-------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `message`                         | Yes         | Name of the configured message/archive type.                                                                                              |
| `config-id`                       | Conditional | The `id` of the target configuration. Mandatory when the message has multiple remote-data configurations; optional otherwise.             |
| `archiveDataReferences`           | Yes         | Non-empty list of archive data references to process.                                                                                     |
| `archiveDataReferences[].id`      | Yes         | Business reference id understood by the source service.                                                                                   |
| `archiveDataReferences[].version` | No          | Business reference version understood by the source service. Required only when the configured data reader endpoint contains `{version}`. |

Responses:

| Status            | Meaning                                                                                                                                         |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `200 OK`          | Job accepted, or the same job was already submitted with identical content.                                                                     |
| `400 Bad Request` | Invalid YAML/request content, unknown archive configuration, unsupported non-remote archive configuration, or an ambiguous/unknown `config-id`. |
| `403 Forbidden`   | Caller does not have `backfilljob:write`.                                                                                                       |
| `409 Conflict`    | A job with the same `jobId` already exists with different content.                                                                              |

Example:

```bash
curl -X PUT "https://pas.example/api/jobs/88dbb65f-9634-4685-bc86-17b72d715d3e" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/yaml" \
  --data-binary @backfill-request.yaml
```

### Get a job report

```http
GET /api/jobs/{jobId}/report
Accept: application/yaml
```

Required role: `backfilljob:read`.

Responses:

| Status | Meaning |
| --- | --- |
| `200 OK` | Report returned as YAML. |
| `403 Forbidden` | Caller does not have `backfilljob:read`. |
| `404 Not Found` | No backfill job exists for the given `jobId`. |

Example report for a completed job:

```yaml
message: JmeDecreeDocumentCreatedEvent
config-id: decree-document
job-state: completed
job-result: partially-succeeded
job-id: 88dbb65f-9634-4685-bc86-17b72d715d3e
started: 2026-05-08T07:26:37.123Z
report-created: 2026-05-08T07:30:15.456Z
started-by-name: John Doe
started-by-ext_id: "287365"
archiveDataReferences:
  - id: DOC-2024-001
    version: 1
    state: succeeded
  - id: DOC-2024-002
    version: 1
    state: failed
    error:
      message: Failed reading artifact from source service
      traceId: 4bf92f3577b34da6a3ce929d0e0e4736
```

For a running job, `job-state` is `open`, `job-result` and `report-created` are not present, and individual references may still have state `open`.

## States and results

Job states:

| State | Description |
| --- | --- |
| `open` | At least one task is still open. |
| `completed` | All tasks finished. |

Task states:

| State | Description |
| --- | --- |
| `open` | The reference has not finished processing yet. |
| `succeeded` | The archive data was read, validated, stored, and the archived artifact event was published. |
| `failed` | Processing failed. The report contains an error message and, when available, a trace id. |

Job results:

| Result | Description |
| --- | --- |
| `succeeded` | All tasks succeeded. |
| `partially-succeeded` | At least one task succeeded and at least one task failed. |
| `failed` | All finished tasks failed. |

## Security

The endpoints use semantic roles:

| Operation | Required role |
| --- | --- |
| Submit a job | `backfilljob:write` |
| Read a report | `backfilljob:read` |

The report includes submitter information from the JWT claims `name` and `ext_id` when they are available.

## Operational notes

- Choose a stable UUID for each job. This allows safe retries of the same submission.
- Use small enough batches that reports remain easy to inspect and failed references can be resubmitted deliberately.
- Backfill processing is asynchronous. A successful submit response only means that the job and tasks were persisted and commands were published.
- Failed tasks are not automatically retried by submitting the same job again, because identical submissions are treated as idempotent once the job exists. Create a new job for references that should be retried.
- The submitted `config-id` is part of the job content for idempotency: resubmitting the same `jobId` with a different (
  or newly added) `config-id` is treated as different content and returns `409 Conflict`. Use a new `jobId` in that
  case.
- If the source service returns no archive data for a reference, the command processing logs this and leaves the task open. Check source-service data and PAS logs before resubmitting.
- Ensure the configured source endpoint and OAuth client can read the historical data for every requested reference.
- The backfill topic is both the publisher and consumer topic for PAS. Configure contracts and access accordingly.
- Backfilled artifacts use a backfill-specific event idempotence id derived from the `CreateArtifactCommand` identity,
  which differs from the discriminated idempotence id of the live message-driven path. This is intentional (one command
  produces exactly one artifact, so the id is unique and stable across retries); an artifact archived both live and via
  backfill is therefore not de-duplicated across the two paths by idempotence id.
