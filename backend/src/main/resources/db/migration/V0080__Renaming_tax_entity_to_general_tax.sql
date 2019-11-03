alter table tax
    rename to general_tax;

alter table general_tax
    rename constraint tax_workspace_fk to general_tax_workspace_fk;

alter table income
    alter column tax_amount rename to general_tax_amount;

alter table income
    alter column tax_id rename to general_tax_id;

alter table income
    alter column tax_rate_in_bps rename to general_tax_rate_in_bps;

alter table income
    rename constraint income_tax_fk to income_general_tax_fk;

alter table expense
    alter column tax_amount rename to general_tax_amount;

alter table expense
    alter column tax_id rename to general_tax_id;

alter table expense
    alter column tax_rate_in_bps rename to general_tax_rate_in_bps;

alter table expense
    rename constraint expense_tax_fk to expense_general_tax_fk;

alter table invoice
    alter column tax_id rename to general_tax_id;

alter table invoice
    rename constraint invoice_tax_fk to invoice_general_tax_fk;
