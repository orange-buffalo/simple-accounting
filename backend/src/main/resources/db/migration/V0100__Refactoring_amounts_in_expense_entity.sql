-- status and amounts will be migrated by recalculating then during the startup

--  use_different_exchange_rate_for_income_tax_purposes
alter table expense
    add column use_different_exchange_rate_for_income_tax_purposes boolean;

update expense
set use_different_exchange_rate_for_income_tax_purposes =
        (amount_in_default_currency != actual_amount_in_default_currency);

alter table expense
    alter column use_different_exchange_rate_for_income_tax_purposes set not null;

-- status

alter table expense
    add column status varchar(255);

update expense
set status = 'PENDING_CONVERSION';

alter table expense
    alter column status set not null;

--- income_taxable_original_amount_in_default_currency

alter table expense
    alter column actual_amount_in_default_currency rename to income_taxable_original_amount_in_default_currency;

alter table expense
    alter column income_taxable_original_amount_in_default_currency drop not null;

update expense
set income_taxable_original_amount_in_default_currency = null
where income_taxable_original_amount_in_default_currency = 0;

-- income_taxable_adjusted_amount_in_default_currency

alter table expense
    alter column reported_amount_in_default_currency rename to income_taxable_adjusted_amount_in_default_currency;

alter table expense
    alter column income_taxable_adjusted_amount_in_default_currency drop not null;

update expense
set income_taxable_adjusted_amount_in_default_currency = null
where income_taxable_adjusted_amount_in_default_currency = 0;

-- converted_original_amount_in_default_currency

alter table expense
    alter column amount_in_default_currency rename to converted_original_amount_in_default_currency;

alter table expense
    alter column converted_original_amount_in_default_currency drop not null;

update expense
set converted_original_amount_in_default_currency = null
where converted_original_amount_in_default_currency = 0;

-- converted_adjusted_amount_in_default_currency

alter table expense
    add column converted_adjusted_amount_in_default_currency bigint;
