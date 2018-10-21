create sequence hibernate_sequence start with 1 increment by 1
create table category (id bigint not null, version integer not null, description varchar(1024), expense boolean not null, income boolean not null, name varchar(255) not null, workspace_id bigint not null, primary key (id))
create table document (id bigint not null, version integer not null, date_uploaded timestamp not null, name varchar(255) not null, notes varchar(1024), storage_provider_id varchar(255) not null, storage_provider_location varchar(2048), workspace_id bigint not null, primary key (id))
create table platform_user (id bigint not null, version integer not null, is_admin boolean not null, password_hash varchar(255) not null, user_name varchar(255) not null, primary key (id))
create table workspace (id bigint not null, version integer not null, default_currency varchar(255) not null, multi_currency_enabled boolean not null, name varchar(255) not null, tax_enabled boolean not null, owner_id bigint not null, primary key (id))
alter table category add constraint category_workspace_fk foreign key (workspace_id) references workspace
alter table document add constraint document_workspace_fk foreign key (workspace_id) references workspace
alter table workspace add constraint workspace_owner_fk foreign key (owner_id) references platform_user