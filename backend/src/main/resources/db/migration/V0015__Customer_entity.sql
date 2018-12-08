create table customer (
  id bigint not null,
  version integer not null,
  name varchar(255) not null,
  workspace_id bigint not null,
  primary key (id),

  constraint customer_workspace_fk foreign key (workspace_id) references workspace
)
