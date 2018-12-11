create table tax_payment (
  id bigint not null primary key,
  version integer not null,
  amount bigint not null,
  date_paid date not null,
  time_recorded timestamp not null,
  notes varchar(1024),
  workspace_id bigint not null,
  title varchar(255) not null,

  constraint tax_payment_workspace_fk foreign key (workspace_id) references workspace
);

create table tax_payment_attachments (
  tax_payment_id bigint not null,
  document_id bigint not null,

  constraint tax_payment_attachments_document_fk foreign key (document_id) references document,
  constraint tax_payment_attachments_tax_payment_fk foreign key (tax_payment_id) references tax_payment,

  primary key (tax_payment_id, document_id)
);
