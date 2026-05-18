create table "DOCUMENTS_MIGRATION" (
    "ID"                       varchar(10) not null,
    "VERSION"                  integer     not null,
    "CREATED_AT"               timestamp   not null,
    "USER_ID"                  varchar(10) not null,
    "MIGRATED_DOCUMENTS_COUNT" integer     not null
);

alter table "DOCUMENTS_MIGRATION"
    add primary key ("ID");

alter table "DOCUMENTS_MIGRATION"
    add constraint "DOCUMENTS_MIGRATION_USER_FK" foreign key ("USER_ID") references "PLATFORM_USER" ("ID");

create table "DOCUMENTS_MIGRATION_DOCUMENT" (
    "MIGRATION_ID" varchar(10) not null,
    "DOCUMENT_ID"  varchar(10) not null
);

alter table "DOCUMENTS_MIGRATION_DOCUMENT"
    add primary key ("MIGRATION_ID", "DOCUMENT_ID");

alter table "DOCUMENTS_MIGRATION_DOCUMENT"
    add constraint "DOCUMENTS_MIGRATION_DOCUMENT_MIGRATION_FK" foreign key ("MIGRATION_ID") references "DOCUMENTS_MIGRATION" ("ID");

alter table "DOCUMENTS_MIGRATION_DOCUMENT"
    add constraint "DOCUMENTS_MIGRATION_DOCUMENT_DOCUMENT_FK" foreign key ("DOCUMENT_ID") references "DOCUMENT" ("ID");
