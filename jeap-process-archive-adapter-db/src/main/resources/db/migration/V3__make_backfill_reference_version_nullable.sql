alter table backfill_task
    alter column reference_version drop not null;
