alter table tax_payment
    rename to income_tax_payment;

alter table income_tax_payment
    rename constraint tax_payment_workspace_fk to income_tax_payment_workspace_fk;

alter table tax_payment_attachments
    rename to income_tax_payment_attachments;

alter table income_tax_payment_attachments
    rename constraint tax_payment_attachments_tax_payment_fk to income_tax_payment_attachments_tax_payment_fk;

alter table income_tax_payment_attachments
    rename constraint tax_payment_attachments_document_fk to income_tax_payment_attachments_document_fk;

alter table income_tax_payment_attachments
    alter column tax_payment_id rename to income_tax_payment_id;
