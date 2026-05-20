alter table "DOCUMENTS_MIGRATION"
    add column "UPLOAD_STORAGE_ID" varchar(255) not null default 'noop';

alter table "DOCUMENTS_MIGRATION"
    alter column "UPLOAD_STORAGE_ID" drop default;
