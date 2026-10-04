create table wise_integration_settings (
    id character varying(10) not null primary key,
    version integer not null,
    created_at timestamp with time zone not null,
    workspace_id character varying(10) not null references workspace(id),
    token character varying(2000) not null,
    constraint wise_integration_workspace_uq unique (workspace_id)
);

create table wise_integration_account (
    settings_id character varying(10) not null references wise_integration_settings(id) on delete cascade,
    profile_id bigint not null,
    account_id bigint not null,
    currency character varying(3) not null,
    primary key (settings_id, profile_id, account_id)
);
