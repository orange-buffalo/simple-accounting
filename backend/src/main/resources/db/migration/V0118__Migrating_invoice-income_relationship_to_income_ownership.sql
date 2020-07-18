alter table income
    add column linked_invoice_id bigint;

alter table income
    add constraint income_linked_invoice_fk
        foreign key (linked_invoice_id) references invoice;

update income inc
set linked_invoice_id = (
    select inv.id
    from invoice inv
    where inv.income_id = inc.id
    );

alter table invoice
    drop column income_id;
