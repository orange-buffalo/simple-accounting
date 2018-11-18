create sequence hibernate_sequence start with 1 increment by 1
create table category (id bigint not null, version integer not null, description varchar(1024), expense boolean not null, income boolean not null, name varchar(255) not null, workspace_id bigint not null, primary key (id))
create table document (id bigint not null, version integer not null, name varchar(255) not null, notes varchar(1024), storage_provider_id varchar(255) not null, storage_provider_location varchar(2048), time_uploaded timestamp not null, workspace_id bigint not null, primary key (id))
create table expense (id bigint not null, version integer not null, actual_amount_in_default_currency bigint not null, amount_in_default_currency bigint not null, currency varchar(3) not null, date_paid date not null, notes varchar(1024), original_amount bigint not null, percent_on_business integer not null, reported_amount_in_default_currency bigint not null, time_recorded timestamp not null, category_id bigint not null, primary key (id))
create table expense_attachments (expense_id bigint not null, document_id bigint not null)
create table platform_user (id bigint not null, version integer not null, is_admin boolean not null, password_hash varchar(255) not null, user_name varchar(255) not null, primary key (id))
create table workspace (id bigint not null, version integer not null, default_currency varchar(255) not null, multi_currency_enabled boolean not null, name varchar(255) not null, tax_enabled boolean not null, owner_id bigint not null, primary key (id))
alter table category add constraint category_workspace_fk foreign key (workspace_id) references workspace
alter table document add constraint document_workspace_fk foreign key (workspace_id) references workspace
alter table expense add constraint expense_category_fk foreign key (category_id) references category
alter table expense_attachments add constraint expense_attachments_document_fk foreign key (document_id) references document
alter table expense_attachments add constraint expense_attachments_expense_fk foreign key (expense_id) references expense
alter table workspace add constraint workspace_owner_fk foreign key (owner_id) references platform_user