-- Add CREATED_AT column to all entity tables

alter table "WORKSPACE"
    add column "CREATED_AT" timestamp;

alter table "INCOME"
    add column "CREATED_AT" timestamp;

alter table "CUSTOMER"
    add column "CREATED_AT" timestamp;

alter table "CATEGORY"
    add column "CREATED_AT" timestamp;

alter table "EXPENSE"
    add column "CREATED_AT" timestamp;

alter table "DOCUMENT"
    add column "CREATED_AT" timestamp;

alter table "INVOICE"
    add column "CREATED_AT" timestamp;

alter table "GENERAL_TAX"
    add column "CREATED_AT" timestamp;

alter table "REFRESH_TOKEN"
    add column "CREATED_AT" timestamp;

alter table "PLATFORM_USER"
    add column "CREATED_AT" timestamp;

alter table "SAVED_WORKSPACE_ACCESS_TOKEN"
    add column "CREATED_AT" timestamp;

alter table "GOOGLE_DRIVE_STORAGE_INTEGRATION"
    add column "CREATED_AT" timestamp;

alter table "PERSISTENT_OAUTH2_AUTHORIZED_CLIENT"
    add column "CREATED_AT" timestamp;

alter table "WORKSPACE_ACCESS_TOKEN"
    add column "CREATED_AT" timestamp;

alter table "INCOME_TAX_PAYMENT"
    add column "CREATED_AT" timestamp;

alter table "USER_ACTIVATION_TOKEN"
    add column "CREATED_AT" timestamp;

-- Populate existing rows with incremental timestamps based on ID
-- Using 1999-03-28 as base date (MOCK_DATE from tests)
-- Each ID gets base + (ID * 1 second)

update "WORKSPACE"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

update "INCOME"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

update "CUSTOMER"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

update "CATEGORY"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

update "EXPENSE"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

update "DOCUMENT"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

update "INVOICE"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

update "GENERAL_TAX"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

update "REFRESH_TOKEN"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

update "PLATFORM_USER"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

update "SAVED_WORKSPACE_ACCESS_TOKEN"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

update "GOOGLE_DRIVE_STORAGE_INTEGRATION"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

update "PERSISTENT_OAUTH2_AUTHORIZED_CLIENT"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

update "WORKSPACE_ACCESS_TOKEN"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

update "INCOME_TAX_PAYMENT"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

update "USER_ACTIVATION_TOKEN"
set "CREATED_AT" = dateadd('SECOND', "ID", timestamp '1999-03-28 00:00:00')
where "CREATED_AT" is null;

-- Make CREATED_AT not null after populating existing rows

alter table "WORKSPACE"
    alter column "CREATED_AT" set not null;

alter table "INCOME"
    alter column "CREATED_AT" set not null;

alter table "CUSTOMER"
    alter column "CREATED_AT" set not null;

alter table "CATEGORY"
    alter column "CREATED_AT" set not null;

alter table "EXPENSE"
    alter column "CREATED_AT" set not null;

alter table "DOCUMENT"
    alter column "CREATED_AT" set not null;

alter table "INVOICE"
    alter column "CREATED_AT" set not null;

alter table "GENERAL_TAX"
    alter column "CREATED_AT" set not null;

alter table "REFRESH_TOKEN"
    alter column "CREATED_AT" set not null;

alter table "PLATFORM_USER"
    alter column "CREATED_AT" set not null;

alter table "SAVED_WORKSPACE_ACCESS_TOKEN"
    alter column "CREATED_AT" set not null;

alter table "GOOGLE_DRIVE_STORAGE_INTEGRATION"
    alter column "CREATED_AT" set not null;

alter table "PERSISTENT_OAUTH2_AUTHORIZED_CLIENT"
    alter column "CREATED_AT" set not null;

alter table "WORKSPACE_ACCESS_TOKEN"
    alter column "CREATED_AT" set not null;

alter table "INCOME_TAX_PAYMENT"
    alter column "CREATED_AT" set not null;

alter table "USER_ACTIVATION_TOKEN"
    alter column "CREATED_AT" set not null;
