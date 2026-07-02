alter table backfill_job
    add column config_id varchar(255);

alter table backfill_job
    drop column topic_name;
