alter table expense
  alter column percent_on_business_in_bps
  rename to percent_on_business;

update expense
  set percent_on_business = percent_on_business / 100;
