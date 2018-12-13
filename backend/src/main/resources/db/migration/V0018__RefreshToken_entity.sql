create table refresh_token (
  id bigint not null primary key,
  version integer not null,
  expiration_time timestamp not null,
  token varchar(2048) not null,
  user_id bigint not null,

  constraint refresh_token_user_fk foreign key (user_id) references platform_user
);
