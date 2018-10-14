create table workspace (
  id bigint not null,
  version int not null,
  name varchar(255) not null,
  owner_id bigint not null,
  tax_enabled boolean not null,
  multi_currency_enabled boolean not null,
  default_currency varchar(3) not null
);