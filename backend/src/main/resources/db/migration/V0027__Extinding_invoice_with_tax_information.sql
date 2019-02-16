alter table invoice
  add column tax_id bigint;

alter table invoice
  add constraint invoice_tax_fk foreign key (tax_id) references tax;
