-- Remove TIME_RECORDED columns as they are replaced by CREATED_AT from the base entity

alter table "EXPENSE"
    drop column "TIME_RECORDED";

alter table "INCOME"
    drop column "TIME_RECORDED";

alter table "INVOICE"
    drop column "TIME_RECORDED";

alter table "INCOME_TAX_PAYMENT"
    drop column "TIME_RECORDED";
