alter table expense
  add column tax_id bigint;

alter table expense
  add column tax_amount bigint;

alter table expense
  add column tax_rate_in_bps integer;

alter table expense
  add constraint expense_tax_fk foreign key (tax_id) references tax;
