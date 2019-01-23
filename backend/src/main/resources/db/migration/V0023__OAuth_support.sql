create table persistent_oauth2_authorized_client (
  id bigint not null,
  version integer not null,
  access_token varchar(255) not null,
  access_token_expires_at timestamp,
  access_token_issued_at timestamp,
  client_registration_id varchar(255) not null,
  refresh_token varchar(255),
  refresh_token_issued_at timestamp,
  user_name varchar(255) not null,
  primary key (id)
);

create table persistent_oauth2_authorized_client_access_token_scopes (
  client_id bigint not null,
  access_token_scopes varchar(255),

  constraint pauth2ac_access_token_scopes_scopes_client_fk foreign key (client_id) references persistent_oauth2_authorized_client
);

create table persistent_oauth2_authorization_request (
  id bigint not null,
  version integer not null,
  client_registration_id varchar(255) not null,
  create_when timestamp not null,
  state varchar(512) not null,
  owner_id bigint not null,
  primary key (id),

  constraint persistent_oauth2_authorization_request_owner_fk foreign key (owner_id) references platform_user
);
