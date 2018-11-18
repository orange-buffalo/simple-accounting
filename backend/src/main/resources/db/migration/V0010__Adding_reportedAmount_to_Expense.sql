alter table expense
  add column reported_amount_in_default_currency bigint;

update expense
set reported_amount_in_default_currency = actual_amount_in_default_currency;

alter table expense
  alter column reported_amount_in_default_currency set not null; 