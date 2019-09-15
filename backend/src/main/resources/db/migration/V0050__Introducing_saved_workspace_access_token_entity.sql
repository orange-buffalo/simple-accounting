create table saved_workspace_access_token (
    id                        bigint  not null,
    version                   integer not null,
    owner_id                  bigint  not null,
    workspace_access_token_id bigint  not null,
    primary key (id),
    constraint saved_ws_access_token_owner_fk
        foreign key (owner_id) references platform_user,
    constraint saved_ws_access_token_ws_access_token_fk
        foreign key (workspace_access_token_id) references workspace_access_token
);
