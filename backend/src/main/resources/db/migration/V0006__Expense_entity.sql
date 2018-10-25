create table expense (
  id bigint not null primary key,
  version integer not null,
  actual_amount_in_default_currency bigint not null,
  amount_in_default_currency bigint not null,
  currency varchar(3) not null,
  date_paid timestamp not null,
  date_recorded timestamp not null,
  notes varchar(1024),
  original_amount bigint not null,
  percent_on_business_in_bps integer not null,
  category_id bigint not null,
  workspace_id bigint not null,

  constraint expense_category_fk foreign key (category_id) references category,
  constraint expense_workspace_fk foreign key (workspace_id) references workspace
);

create table expense_attachments (
  expense_id bigint not null,
  document_id bigint not null,

  constraint expense_attachments_document_fk foreign key (document_id) references document,
  constraint expense_attachments_expense_fk foreign key (expense_id) references expense
);
