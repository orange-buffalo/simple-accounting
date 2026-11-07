alter table wise_integration_settings
    add column create_income_from_invoices boolean,
    add column populate_income_converted_amounts boolean;
update wise_integration_settings
set create_income_from_invoices = true, populate_income_converted_amounts = true;
alter table wise_integration_settings
    alter column create_income_from_invoices set not null;
alter table wise_integration_settings
    alter column populate_income_converted_amounts set not null;

alter table account_statement_record
    add column linked_income_id character varying(10) references income(id) on delete set null;
create index account_statement_income_idx on account_statement_record(linked_income_id);
