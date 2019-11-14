-- status and amounts will be migrated by recalculating then during the startup

--  use_different_exchange_rate_for_income_tax_purposes
alter table income
    add column use_different_exchange_rate_for_income_tax_purposes boolean;

update income
set use_different_exchange_rate_for_income_tax_purposes =
        (amount_in_default_currency != reported_amount_in_default_currency);

alter table income
    alter column use_different_exchange_rate_for_income_tax_purposes set not null;

-- status

alter table income
    add column status varchar(255);

update income
set status = 'PENDING_CONVERSION';

alter table income
    alter column status set not null;

--- income_taxable_original_amount_in_default_currency

alter table income
    alter column reported_amount_in_default_currency rename to income_taxable_original_amount_in_default_currency;

alter table income
    alter column income_taxable_original_amount_in_default_currency drop not null;

update income
set income_taxable_original_amount_in_default_currency = null
where income_taxable_original_amount_in_default_currency = 0;

-- income_taxable_adjusted_amount_in_default_currency

alter table income
    add column income_taxable_adjusted_amount_in_default_currency bigint;

-- converted_original_amount_in_default_currency

alter table income
    alter column amount_in_default_currency rename to converted_original_amount_in_default_currency;

alter table income
    alter column converted_original_amount_in_default_currency drop not null;

update income
set converted_original_amount_in_default_currency = null
where converted_original_amount_in_default_currency = 0;

-- converted_adjusted_amount_in_default_currency

alter table income
    add column converted_adjusted_amount_in_default_currency bigint;
