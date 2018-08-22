create sequence hibernate_sequence;

create table platform_user (
  id bigint not null,
  version int not null
);