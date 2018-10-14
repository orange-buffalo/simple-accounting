alter table platform_user
  add primary key (id);

alter table workspace
  add primary key (id);

alter table workspace
  add constraint workspace_owner_fk foreign key (owner_id) references platform_user;