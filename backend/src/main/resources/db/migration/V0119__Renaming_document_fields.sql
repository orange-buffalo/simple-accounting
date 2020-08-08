alter table document
    alter column storage_provider_id rename to storage_id;

alter table document
    alter column storage_provider_location rename to storage_location;
