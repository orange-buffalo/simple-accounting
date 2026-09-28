alter table "DOCUMENT" drop constraint "DOCUMENT_WORKSPACE_FK";
alter table "EXPENSE" drop constraint "EXPENSE_GENERAL_TAX_FK";
alter table "INCOME" drop constraint "INCOME_LINKED_INVOICE_FK";
alter table "INVOICE" drop constraint "INVOICE_GENERAL_TAX_FK";
alter table "SAVED_WORKSPACE_ACCESS_TOKEN" drop constraint "SAVED_WS_ACCESS_TOKEN_OWNER_FK";
alter table "INCOME_TAX_PAYMENT" drop constraint "INCOME_TAX_PAYMENT_WORKSPACE_FK";
alter table "INVOICE_ATTACHMENTS" drop constraint "INVOICE_ATTACHMENTS_INVOICE_FK";
alter table "INCOME" drop constraint "INCOME_GENERAL_TAX_FK";
alter table "PERSISTENT_OAUTH2_AUTHORIZED_CLIENT_ACCESS_TOKEN_SCOPES" drop constraint "PAUTH2AC_ACCESS_TOKEN_SCOPES_SCOPES_CLIENT_FK";
alter table "SAVED_WORKSPACE_ACCESS_TOKEN" drop constraint "SAVED_WS_ACCESS_TOKEN_WS_ACCESS_TOKEN_FK";
alter table "WORKSPACE" drop constraint "WORKSPACE_OWNER_FK";
alter table "EXPENSE_ATTACHMENTS" drop constraint "EXPENSE_ATTACHMENTS_DOCUMENT_FK";
alter table "EXPENSE" drop constraint "EXPENSE_WORKSPACE_FK";
alter table "INCOME_ATTACHMENTS" drop constraint "INCOME_ATTACHMENTS_DOCUMENT_FK";
alter table "INCOME" drop constraint "INCOME_WORKSPACE_FK";
alter table "INCOME" drop constraint "INCOME_CATEGORY_FK";
alter table "CUSTOMER" drop constraint "CUSTOMER_WORKSPACE_FK";
alter table "INVOICE" drop constraint "INVOICE_CUSTOMER_FK";
alter table "INCOME_TAX_PAYMENT_ATTACHMENTS" drop constraint "INCOME_TAX_PAYMENT_ATTACHMENTS_DOCUMENT_FK";
alter table "CATEGORY" drop constraint "CATEGORY_WORKSPACE_FK";
alter table "GENERAL_TAX" drop constraint "GENERAL_TAX_WORKSPACE_FK";
alter table "INCOME_ATTACHMENTS" drop constraint "INCOME_ATTACHMENTS_INCOME_FK";
alter table "WORKSPACE_ACCESS_TOKEN" drop constraint "WORKSPACE_ACCESS_TOKEN_WORKSPACE_FK";
alter table "INVOICE_ATTACHMENTS" drop constraint "INVOICE_ATTACHMENTS_DOCUMENT_FK";
alter table "EXPENSE_ATTACHMENTS" drop constraint "EXPENSE_ATTACHMENTS_EXPENSE_FK";
alter table "REFRESH_TOKEN" drop constraint "REFRESH_TOKEN_USER_FK";
alter table "EXPENSE" drop constraint "EXPENSE_CATEGORY_FK";
alter table "GOOGLE_DRIVE_STORAGE_INTEGRATION" drop constraint "GDRIVE_STORAGE_INTEGRATION_USER_FK";
alter table "INCOME_TAX_PAYMENT_ATTACHMENTS" drop constraint "INCOME_TAX_PAYMENT_ATTACHMENTS_TAX_PAYMENT_FK";
alter table "USER_ACTIVATION_TOKEN" drop constraint "USER_ACTIVATION_TOKEN_USER_FK";

alter table "GOOGLE_DRIVE_STORAGE_INTEGRATION" drop constraint "GOOGLE_DRIVE_STORAGE_INTEGRATION_UQ";
alter table "USER_ACTIVATION_TOKEN" drop constraint "USER_ACTIVATION_TOKEN_USER_UQ";

alter table "WORKSPACE" drop primary key;
alter table "INCOME" drop primary key;
alter table "CUSTOMER" drop primary key;
alter table "CATEGORY" drop primary key;
alter table "EXPENSE" drop primary key;
alter table "DOCUMENT" drop primary key;
alter table "EXPENSE_ATTACHMENTS" drop primary key;
alter table "INCOME_ATTACHMENTS" drop primary key;
alter table "INVOICE" drop primary key;
alter table "GENERAL_TAX" drop primary key;
alter table "REFRESH_TOKEN" drop primary key;
alter table "INVOICE_ATTACHMENTS" drop primary key;
alter table "PLATFORM_USER" drop primary key;
alter table "INCOME_TAX_PAYMENT_ATTACHMENTS" drop primary key;
alter table "SAVED_WORKSPACE_ACCESS_TOKEN" drop primary key;
alter table "GOOGLE_DRIVE_STORAGE_INTEGRATION" drop primary key;
alter table "PERSISTENT_OAUTH2_AUTHORIZED_CLIENT" drop primary key;
alter table "WORKSPACE_ACCESS_TOKEN" drop primary key;
alter table "INCOME_TAX_PAYMENT" drop primary key;

alter table "PLATFORM_USER" alter column "ID" varchar(10);
alter table "WORKSPACE" alter column "ID" varchar(10);
alter table "WORKSPACE" alter column "OWNER_ID" varchar(10);
alter table "INCOME" alter column "ID" varchar(10);
alter table "INCOME" alter column "CATEGORY_ID" varchar(10);
alter table "INCOME" alter column "WORKSPACE_ID" varchar(10);
alter table "INCOME" alter column "GENERAL_TAX_ID" varchar(10);
alter table "INCOME" alter column "LINKED_INVOICE_ID" varchar(10);
alter table "CUSTOMER" alter column "ID" varchar(10);
alter table "CUSTOMER" alter column "WORKSPACE_ID" varchar(10);
alter table "CATEGORY" alter column "ID" varchar(10);
alter table "CATEGORY" alter column "WORKSPACE_ID" varchar(10);
alter table "EXPENSE" alter column "ID" varchar(10);
alter table "EXPENSE" alter column "CATEGORY_ID" varchar(10);
alter table "EXPENSE" alter column "WORKSPACE_ID" varchar(10);
alter table "EXPENSE" alter column "GENERAL_TAX_ID" varchar(10);
alter table "DOCUMENT" alter column "ID" varchar(10);
alter table "DOCUMENT" alter column "WORKSPACE_ID" varchar(10);
alter table "EXPENSE_ATTACHMENTS" alter column "EXPENSE_ID" varchar(10);
alter table "EXPENSE_ATTACHMENTS" alter column "DOCUMENT_ID" varchar(10);
alter table "INCOME_ATTACHMENTS" alter column "INCOME_ID" varchar(10);
alter table "INCOME_ATTACHMENTS" alter column "DOCUMENT_ID" varchar(10);
alter table "INVOICE" alter column "ID" varchar(10);
alter table "INVOICE" alter column "CUSTOMER_ID" varchar(10);
alter table "INVOICE" alter column "GENERAL_TAX_ID" varchar(10);
alter table "GENERAL_TAX" alter column "ID" varchar(10);
alter table "GENERAL_TAX" alter column "WORKSPACE_ID" varchar(10);
alter table "REFRESH_TOKEN" alter column "ID" varchar(10);
alter table "REFRESH_TOKEN" alter column "USER_ID" varchar(10);
alter table "INVOICE_ATTACHMENTS" alter column "INVOICE_ID" varchar(10);
alter table "INVOICE_ATTACHMENTS" alter column "DOCUMENT_ID" varchar(10);
alter table "PERSISTENT_OAUTH2_AUTHORIZED_CLIENT_ACCESS_TOKEN_SCOPES" alter column "CLIENT_ID" varchar(10);
alter table "INCOME_TAX_PAYMENT_ATTACHMENTS" alter column "INCOME_TAX_PAYMENT_ID" varchar(10);
alter table "INCOME_TAX_PAYMENT_ATTACHMENTS" alter column "DOCUMENT_ID" varchar(10);
alter table "SAVED_WORKSPACE_ACCESS_TOKEN" alter column "ID" varchar(10);
alter table "SAVED_WORKSPACE_ACCESS_TOKEN" alter column "OWNER_ID" varchar(10);
alter table "SAVED_WORKSPACE_ACCESS_TOKEN" alter column "WORKSPACE_ACCESS_TOKEN_ID" varchar(10);
alter table "GOOGLE_DRIVE_STORAGE_INTEGRATION" alter column "ID" varchar(10);
alter table "GOOGLE_DRIVE_STORAGE_INTEGRATION" alter column "USER_ID" varchar(10);
alter table "PERSISTENT_OAUTH2_AUTHORIZED_CLIENT" alter column "ID" varchar(10);
alter table "WORKSPACE_ACCESS_TOKEN" alter column "ID" varchar(10);
alter table "WORKSPACE_ACCESS_TOKEN" alter column "WORKSPACE_ID" varchar(10);
alter table "INCOME_TAX_PAYMENT" alter column "ID" varchar(10);
alter table "INCOME_TAX_PAYMENT" alter column "WORKSPACE_ID" varchar(10);
alter table "USER_ACTIVATION_TOKEN" alter column "ID" varchar(10);
alter table "USER_ACTIVATION_TOKEN" alter column "USER_ID" varchar(10);

alter table "WORKSPACE" add primary key ("ID");
alter table "INCOME" add primary key ("ID");
alter table "CUSTOMER" add primary key ("ID");
alter table "CATEGORY" add primary key ("ID");
alter table "EXPENSE" add primary key ("ID");
alter table "DOCUMENT" add primary key ("ID");
alter table "EXPENSE_ATTACHMENTS" add primary key ("EXPENSE_ID", "DOCUMENT_ID");
alter table "INCOME_ATTACHMENTS" add primary key ("INCOME_ID", "DOCUMENT_ID");
alter table "INVOICE" add primary key ("ID");
alter table "GENERAL_TAX" add primary key ("ID");
alter table "REFRESH_TOKEN" add primary key ("ID");
alter table "INVOICE_ATTACHMENTS" add primary key ("INVOICE_ID", "DOCUMENT_ID");
alter table "PLATFORM_USER" add primary key ("ID");
alter table "INCOME_TAX_PAYMENT_ATTACHMENTS" add primary key ("INCOME_TAX_PAYMENT_ID", "DOCUMENT_ID");
alter table "SAVED_WORKSPACE_ACCESS_TOKEN" add primary key ("ID");
alter table "GOOGLE_DRIVE_STORAGE_INTEGRATION" add primary key ("ID");
alter table "PERSISTENT_OAUTH2_AUTHORIZED_CLIENT" add primary key ("ID");
alter table "WORKSPACE_ACCESS_TOKEN" add primary key ("ID");
alter table "INCOME_TAX_PAYMENT" add primary key ("ID");

alter table "GOOGLE_DRIVE_STORAGE_INTEGRATION"
    add constraint "GOOGLE_DRIVE_STORAGE_INTEGRATION_UQ" unique ("USER_ID");
alter table "USER_ACTIVATION_TOKEN"
    add constraint "USER_ACTIVATION_TOKEN_USER_UQ" unique ("USER_ID");

alter table "DOCUMENT"
    add constraint "DOCUMENT_WORKSPACE_FK" foreign key ("WORKSPACE_ID") references "WORKSPACE" ("ID");
alter table "EXPENSE"
    add constraint "EXPENSE_GENERAL_TAX_FK" foreign key ("GENERAL_TAX_ID") references "GENERAL_TAX" ("ID");
alter table "INCOME"
    add constraint "INCOME_LINKED_INVOICE_FK" foreign key ("LINKED_INVOICE_ID") references "INVOICE" ("ID");
alter table "INVOICE"
    add constraint "INVOICE_GENERAL_TAX_FK" foreign key ("GENERAL_TAX_ID") references "GENERAL_TAX" ("ID");
alter table "SAVED_WORKSPACE_ACCESS_TOKEN"
    add constraint "SAVED_WS_ACCESS_TOKEN_OWNER_FK" foreign key ("OWNER_ID") references "PLATFORM_USER" ("ID");
alter table "INCOME_TAX_PAYMENT"
    add constraint "INCOME_TAX_PAYMENT_WORKSPACE_FK" foreign key ("WORKSPACE_ID") references "WORKSPACE" ("ID");
alter table "INVOICE_ATTACHMENTS"
    add constraint "INVOICE_ATTACHMENTS_INVOICE_FK" foreign key ("INVOICE_ID") references "INVOICE" ("ID");
alter table "INCOME"
    add constraint "INCOME_GENERAL_TAX_FK" foreign key ("GENERAL_TAX_ID") references "GENERAL_TAX" ("ID");
alter table "PERSISTENT_OAUTH2_AUTHORIZED_CLIENT_ACCESS_TOKEN_SCOPES"
    add constraint "PAUTH2AC_ACCESS_TOKEN_SCOPES_SCOPES_CLIENT_FK" foreign key ("CLIENT_ID") references "PERSISTENT_OAUTH2_AUTHORIZED_CLIENT" ("ID") on delete cascade;
alter table "SAVED_WORKSPACE_ACCESS_TOKEN"
    add constraint "SAVED_WS_ACCESS_TOKEN_WS_ACCESS_TOKEN_FK" foreign key ("WORKSPACE_ACCESS_TOKEN_ID") references "WORKSPACE_ACCESS_TOKEN" ("ID");
alter table "WORKSPACE"
    add constraint "WORKSPACE_OWNER_FK" foreign key ("OWNER_ID") references "PLATFORM_USER" ("ID");
alter table "EXPENSE_ATTACHMENTS"
    add constraint "EXPENSE_ATTACHMENTS_DOCUMENT_FK" foreign key ("DOCUMENT_ID") references "DOCUMENT" ("ID");
alter table "EXPENSE"
    add constraint "EXPENSE_WORKSPACE_FK" foreign key ("WORKSPACE_ID") references "WORKSPACE" ("ID");
alter table "INCOME_ATTACHMENTS"
    add constraint "INCOME_ATTACHMENTS_DOCUMENT_FK" foreign key ("DOCUMENT_ID") references "DOCUMENT" ("ID");
alter table "INCOME"
    add constraint "INCOME_WORKSPACE_FK" foreign key ("WORKSPACE_ID") references "WORKSPACE" ("ID");
alter table "INCOME"
    add constraint "INCOME_CATEGORY_FK" foreign key ("CATEGORY_ID") references "CATEGORY" ("ID");
alter table "CUSTOMER"
    add constraint "CUSTOMER_WORKSPACE_FK" foreign key ("WORKSPACE_ID") references "WORKSPACE" ("ID");
alter table "INVOICE"
    add constraint "INVOICE_CUSTOMER_FK" foreign key ("CUSTOMER_ID") references "CUSTOMER" ("ID");
alter table "INCOME_TAX_PAYMENT_ATTACHMENTS"
    add constraint "INCOME_TAX_PAYMENT_ATTACHMENTS_DOCUMENT_FK" foreign key ("DOCUMENT_ID") references "DOCUMENT" ("ID");
alter table "CATEGORY"
    add constraint "CATEGORY_WORKSPACE_FK" foreign key ("WORKSPACE_ID") references "WORKSPACE" ("ID");
alter table "GENERAL_TAX"
    add constraint "GENERAL_TAX_WORKSPACE_FK" foreign key ("WORKSPACE_ID") references "WORKSPACE" ("ID");
alter table "INCOME_ATTACHMENTS"
    add constraint "INCOME_ATTACHMENTS_INCOME_FK" foreign key ("INCOME_ID") references "INCOME" ("ID");
alter table "WORKSPACE_ACCESS_TOKEN"
    add constraint "WORKSPACE_ACCESS_TOKEN_WORKSPACE_FK" foreign key ("WORKSPACE_ID") references "WORKSPACE" ("ID");
alter table "INVOICE_ATTACHMENTS"
    add constraint "INVOICE_ATTACHMENTS_DOCUMENT_FK" foreign key ("DOCUMENT_ID") references "DOCUMENT" ("ID");
alter table "EXPENSE_ATTACHMENTS"
    add constraint "EXPENSE_ATTACHMENTS_EXPENSE_FK" foreign key ("EXPENSE_ID") references "EXPENSE" ("ID");
alter table "REFRESH_TOKEN"
    add constraint "REFRESH_TOKEN_USER_FK" foreign key ("USER_ID") references "PLATFORM_USER" ("ID");
alter table "EXPENSE"
    add constraint "EXPENSE_CATEGORY_FK" foreign key ("CATEGORY_ID") references "CATEGORY" ("ID");
alter table "GOOGLE_DRIVE_STORAGE_INTEGRATION"
    add constraint "GDRIVE_STORAGE_INTEGRATION_USER_FK" foreign key ("USER_ID") references "PLATFORM_USER" ("ID");
alter table "INCOME_TAX_PAYMENT_ATTACHMENTS"
    add constraint "INCOME_TAX_PAYMENT_ATTACHMENTS_TAX_PAYMENT_FK" foreign key ("INCOME_TAX_PAYMENT_ID") references "INCOME_TAX_PAYMENT" ("ID");
alter table "USER_ACTIVATION_TOKEN"
    add constraint "USER_ACTIVATION_TOKEN_USER_FK" foreign key ("USER_ID") references "PLATFORM_USER" ("ID");

drop sequence "HIBERNATE_SEQUENCE";
