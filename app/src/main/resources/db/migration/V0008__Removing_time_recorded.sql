-- Set CREATED_AT to the actual TIME_RECORDED value before dropping the column,
-- as V0007 used an ID-based approximation instead of the real recorded timestamp.

update "EXPENSE"
set "CREATED_AT" = "TIME_RECORDED";

update "INCOME"
set "CREATED_AT" = "TIME_RECORDED";

update "INVOICE"
set "CREATED_AT" = "TIME_RECORDED";

update "INCOME_TAX_PAYMENT"
set "CREATED_AT" = "TIME_RECORDED";

-- Remove TIME_RECORDED columns as they are replaced by CREATED_AT from the base entity

alter table "EXPENSE"
    drop column "TIME_RECORDED";

alter table "INCOME"
    drop column "TIME_RECORDED";

alter table "INVOICE"
    drop column "TIME_RECORDED";

alter table "INCOME_TAX_PAYMENT"
    drop column "TIME_RECORDED";
