alter table persistent_oauth2_authorized_client
    alter column access_token character varying(2000) not null;

alter table persistent_oauth2_authorized_client
    alter column refresh_token character varying(2000);
