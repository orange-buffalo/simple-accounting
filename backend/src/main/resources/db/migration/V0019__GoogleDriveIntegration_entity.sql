create table google_drive_storage_integration (
  id bigint not null,
  version integer not null,
  auth_state_token varchar(255),
  folder_id varchar(255),
  folder_name varchar(255),
  time_auth_failed timestamp,
  time_auth_requested timestamp,
  time_auth_succeeded timestamp,
  user_id bigint not null,
  primary key (id),
  constraint gdrive_storage_integration_user_fk foreign key (user_id) references platform_user
);
