create table "STANDALONE_DOCUMENT" (
    "ID"          varchar(10)            not null,
    "VERSION"     integer                not null,
    "CREATED_AT"   timestamp              not null,
    "TITLE"       character varying(255) not null,
    "DOCUMENT_ID" varchar(10)            not null
);

alter table "STANDALONE_DOCUMENT"
    add primary key ("ID");

alter table "STANDALONE_DOCUMENT"
    add constraint "STANDALONE_DOCUMENT_DOCUMENT_FK" foreign key ("DOCUMENT_ID") references "DOCUMENT" ("ID");
