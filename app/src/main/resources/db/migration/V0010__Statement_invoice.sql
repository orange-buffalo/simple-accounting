alter table account_statement_record
    add column linked_invoice_id character varying(10) references invoice(id) on delete set null,
    add constraint account_statement_invoice_uq unique (linked_invoice_id);
