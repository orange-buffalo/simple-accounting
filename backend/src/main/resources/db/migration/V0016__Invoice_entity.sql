create table invoice (
  id bigint not null primary key,
  version integer not null,
  income_id bigint,
  customer_id bigint not null,
  title varchar(255) not null,
  time_recorded timestamp not null,
  date_issued date not null,
  date_sent date,
  date_paid date,
  date_cancelled date,
  due_date date not null,
  currency varchar(3) not null,
  amount bigint not null,
  notes varchar(1024),

  constraint invoice_income_fk foreign key (income_id) references income,
  constraint invoice_customer_fk foreign key (customer_id) references customer
);

create table invoice_attachments (
  invoice_id bigint not null,
  document_id bigint not null,
  primary key (invoice_id, document_id),

  constraint invoice_attachments_document_fk foreign key (document_id) references document,
  constraint invoice_attachments_invoice_fk foreign key (invoice_id) references invoice
);

alter table expense_attachments
  add primary key (expense_id, document_id);

alter table income_attachments
  add primary key (income_id, document_id);
