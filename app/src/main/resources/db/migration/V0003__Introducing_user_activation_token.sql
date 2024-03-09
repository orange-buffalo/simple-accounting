create table "USER_ACTIVATION_TOKEN" (
    "ID"         bigint generated by default as identity not null,
    "VERSION"    integer                                 not null,
    "USER_ID"    bigint                                  not null,
    "TOKEN"      varchar(255)                            not null,
    "EXPIRES_AT" timestamp                               not null
)
