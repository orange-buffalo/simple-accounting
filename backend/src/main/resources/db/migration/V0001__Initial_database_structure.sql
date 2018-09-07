create sequence hibernate_sequence;

create table platform_user (
  id bigint not null,
  version int not null,
  user_name varchar(255) not null,
  password_hash varchar(255) not null,
  is_admin boolean
);