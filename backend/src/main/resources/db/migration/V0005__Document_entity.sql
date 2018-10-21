create table document (
  id bigint not null,
  version integer not null,
  date_uploaded timestamp not null,
  name varchar(255) not null,
  notes varchar(1024),
  storage_provider_id varchar(255) not null,
  storage_provider_location varchar(2048),
  workspace_id bigint not null,
  primary key (id),

  constraint document_workspace_fk foreign key (workspace_id) references workspace
)