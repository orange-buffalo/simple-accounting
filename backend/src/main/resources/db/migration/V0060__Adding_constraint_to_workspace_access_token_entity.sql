alter table workspace_access_token
    add constraint workspace_access_token_token_uq unique (token);