alter table persistent_oauth2_authorized_client_access_token_scopes
    drop constraint pauth2ac_access_token_scopes_scopes_client_fk;

alter table persistent_oauth2_authorized_client_access_token_scopes
    add constraint pauth2ac_access_token_scopes_scopes_client_fk
        foreign key (client_id) references persistent_oauth2_authorized_client
            on delete cascade;
