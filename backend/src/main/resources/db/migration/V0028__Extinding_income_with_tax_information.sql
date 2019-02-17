alter table income
  add column tax_id bigint;

alter table income
  add column tax_amount bigint;

alter table income
  add column tax_rate_in_bps integer;

alter table income
  add constraint income_tax_fk foreign key (tax_id) references tax;
