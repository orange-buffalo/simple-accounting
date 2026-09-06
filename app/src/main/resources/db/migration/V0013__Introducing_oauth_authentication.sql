create table "OAUTH_PROVIDER" (
    "ID"                varchar(10)             not null,
    "VERSION"           integer                 not null,
    "CREATED_AT"        timestamp               not null,
    "NAME"              character varying(255)  not null,
    "CLIENT_ID"         character varying(255)  not null,
    "CLIENT_SECRET"     character varying(255)  not null,
    "AUTHORIZATION_URL" character varying(2048) not null,
    "TOKEN_URL"         character varying(2048) not null,
    "USER_INFO_URL"     character varying(2048) not null,
    "USER_ID_ATTRIBUTE" character varying(255)  not null
);

alter table "OAUTH_PROVIDER"
    add primary key ("ID");

alter table "OAUTH_PROVIDER"
    add constraint "OAUTH_PROVIDER_NAME_UQ" unique ("NAME");

create table "OAUTH_PROVIDER_SCOPE" (
    "PROVIDER_ID" varchar(10)            not null,
    "SCOPE"       character varying(255) not null
);

alter table "OAUTH_PROVIDER_SCOPE"
    add primary key ("PROVIDER_ID", "SCOPE");

alter table "OAUTH_PROVIDER_SCOPE"
    add constraint "OAUTH_PROVIDER_SCOPE_PROVIDER_FK" foreign key ("PROVIDER_ID") references "OAUTH_PROVIDER" ("ID") on delete cascade;

create table "USER_OAUTH_IDENTITY" (
    "ID"          varchar(10)            not null,
    "VERSION"     integer                not null,
    "CREATED_AT"  timestamp              not null,
    "USER_ID"     varchar(10)            not null,
    "PROVIDER_ID" varchar(10)            not null,
    "EXTERNAL_ID" character varying(255) not null
);

alter table "USER_OAUTH_IDENTITY"
    add primary key ("ID");

alter table "USER_OAUTH_IDENTITY"
    add constraint "USER_OAUTH_IDENTITY_USER_FK" foreign key ("USER_ID") references "PLATFORM_USER" ("ID");

alter table "USER_OAUTH_IDENTITY"
    add constraint "USER_OAUTH_IDENTITY_PROVIDER_FK" foreign key ("PROVIDER_ID") references "OAUTH_PROVIDER" ("ID");

alter table "USER_OAUTH_IDENTITY"
    add constraint "USER_OAUTH_IDENTITY_USER_PROVIDER_UQ" unique ("USER_ID", "PROVIDER_ID");

alter table "USER_OAUTH_IDENTITY"
    add constraint "USER_OAUTH_IDENTITY_EXTERNAL_ID_UQ" unique ("PROVIDER_ID", "EXTERNAL_ID");

create table "OAUTH_AUTHENTICATION_REQUEST" (
    "ID"                         varchar(10)            not null,
    "VERSION"                    integer                not null,
    "CREATED_AT"                 timestamp              not null,
    "STATE"                      character varying(255) not null,
    "BROWSER_BINDING"            character varying(255) not null,
    "PROVIDER_ID"                varchar(10)            not null,
    "USER_ID"                    varchar(10)            not null,
    "PURPOSE"                    character varying(20)  not null,
    "ISSUE_REFRESH_TOKEN_COOKIE" boolean                not null,
    "EXPIRES_AT"                 timestamp              not null
);

alter table "OAUTH_AUTHENTICATION_REQUEST"
    add primary key ("ID");

alter table "OAUTH_AUTHENTICATION_REQUEST"
    add constraint "OAUTH_AUTHENTICATION_REQUEST_STATE_UQ" unique ("STATE");

alter table "OAUTH_AUTHENTICATION_REQUEST"
    add constraint "OAUTH_AUTHENTICATION_REQUEST_PROVIDER_FK" foreign key ("PROVIDER_ID") references "OAUTH_PROVIDER" ("ID");

alter table "OAUTH_AUTHENTICATION_REQUEST"
    add constraint "OAUTH_AUTHENTICATION_REQUEST_USER_FK" foreign key ("USER_ID") references "PLATFORM_USER" ("ID");
