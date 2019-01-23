alter table google_drive_storage_integration
  drop column auth_state_token;

alter table google_drive_storage_integration
  drop column time_auth_failed;

alter table google_drive_storage_integration
  drop column time_auth_requested;

alter table google_drive_storage_integration
  drop column time_auth_succeeded;