alter table tax_payment
  add column reporting_date date;

update tax_payment
set reporting_date = date_paid;

alter table tax_payment
  alter column reporting_date set not null;