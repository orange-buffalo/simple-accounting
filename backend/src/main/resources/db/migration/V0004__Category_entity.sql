create table category (
  id bigint not null,
  version integer not null,
  description varchar(1024),
  expense boolean not null,
  income boolean not null,
  name varchar(255) not null,
  workspace_id bigint not null,
  primary key (id),

  constraint category_workspace_fk foreign key (workspace_id) references workspace
)
