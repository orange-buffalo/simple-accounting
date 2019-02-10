create table tax (
  id bigint not null primary key,
  version integer not null,
  title varchar(255) not null,
  description varchar(255),
  rate_in_bps integer not null,
  workspace_id bigint not null,

  constraint tax_workspace_fk foreign key (workspace_id) references workspace
);