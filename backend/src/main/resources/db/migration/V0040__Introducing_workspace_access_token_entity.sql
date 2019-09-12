create table workspace_access_token (
    id           bigint       not null,
    version      integer      not null,
    revoked      boolean      not null,
    time_created timestamp    not null,
    token        varchar(255) not null,
    valid_till   timestamp    not null,
    workspace_id bigint       not null,
    primary key (id),
    constraint workspace_access_token_workspace_fk
        foreign key (workspace_id) references workspace
);