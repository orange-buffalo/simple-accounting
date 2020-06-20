alter table google_drive_storage_integration
    add constraint google_drive_storage_integration_uq unique (user_id);
