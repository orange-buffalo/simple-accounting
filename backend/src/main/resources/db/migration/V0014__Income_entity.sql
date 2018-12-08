create table income (
  id bigint not null primary key,
  version integer not null,
  amount_in_default_currency bigint not null,
  currency varchar(3) not null,
  date_received date not null,
  time_recorded timestamp not null,
  notes varchar(1024),
  original_amount bigint not null,
  reported_amount_in_default_currency bigint not null,
  category_id bigint not null,
  title varchar(255) not null,

  constraint income_category_fk foreign key (category_id) references category
);

create table income_attachments (
  income_id bigint not null,
  document_id bigint not null,

  constraint income_attachments_document_fk foreign key (document_id) references document,
  constraint income_attachments_income_fk foreign key (income_id) references income
);
