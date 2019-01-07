alter table income
  alter column category_id set null;

alter table income
  add column workspace_id bigint;

update income i
set i.workspace_id = (select c.workspace_id from category c where c.id = i.category_id);

alter table income
  alter column workspace_id set not null;

alter table income
  add constraint income_workspace_fk foreign key (workspace_id) references workspace;

alter table expense
  alter column category_id set null;

alter table expense
  add column workspace_id bigint;

update expense e
set e.workspace_id = (select c.workspace_id from category c where c.id = e.category_id);

alter table expense
  alter column workspace_id set not null;

alter table expense
  add constraint expense_workspace_fk foreign key (workspace_id) references workspace;
