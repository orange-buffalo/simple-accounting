create sequence hibernate_sequence start with 1 increment by 1
create table category (id bigint not null, version integer not null, description varchar(1024), expense boolean not null, income boolean not null, name varchar(255) not null, workspace_id bigint not null, primary key (id))
create table customer (id bigint not null, version integer not null, name varchar(255) not null, workspace_id bigint not null, primary key (id))
create table document (id bigint not null, version integer not null, name varchar(255) not null, notes varchar(1024), size_in_bytes bigint, storage_provider_id varchar(255) not null, storage_provider_location varchar(2048), time_uploaded timestamp not null, workspace_id bigint not null, primary key (id))
create table expense (id bigint not null, version integer not null, actual_amount_in_default_currency bigint not null, amount_in_default_currency bigint not null, currency varchar(3) not null, date_paid date not null, notes varchar(1024), original_amount bigint not null, percent_on_business integer not null, reported_amount_in_default_currency bigint not null, time_recorded timestamp not null, title varchar(255) not null, category_id bigint not null, primary key (id))
create table expense_attachments (expense_id bigint not null, document_id bigint not null, primary key (expense_id, document_id))
create table income (id bigint not null, version integer not null, amount_in_default_currency bigint not null, currency varchar(3) not null, date_received date not null, notes varchar(1024), original_amount bigint not null, reported_amount_in_default_currency bigint not null, time_recorded timestamp not null, title varchar(255) not null, category_id bigint not null, primary key (id))
create table income_attachments (income_id bigint not null, document_id bigint not null, primary key (income_id, document_id))
create table invoice (id bigint not null, version integer not null, amount bigint not null, currency varchar(3) not null, date_cancelled date, date_issued date not null, date_paid date, date_sent date, due_date date not null, notes varchar(1024), time_recorded timestamp not null, title varchar(255) not null, customer_id bigint not null, income_id bigint, primary key (id))
create table invoice_attachments (invoice_id bigint not null, document_id bigint not null, primary key (invoice_id, document_id))
create table platform_user (id bigint not null, version integer not null, is_admin boolean not null, password_hash varchar(255) not null, user_name varchar(255) not null, primary key (id))
create table tax_payment_attachments (tax_payment_id bigint not null, document_id bigint not null, primary key (tax_payment_id, document_id))
create table tax_payment (id bigint not null, version integer not null, amount bigint not null, date_paid date not null, notes varchar(1024), time_recorded timestamp not null, workspace_id bigint not null, primary key (id))
create table workspace (id bigint not null, version integer not null, default_currency varchar(255) not null, multi_currency_enabled boolean not null, name varchar(255) not null, tax_enabled boolean not null, owner_id bigint not null, primary key (id))
alter table category add constraint category_workspace_fk foreign key (workspace_id) references workspace
alter table customer add constraint customer_workspace_fk foreign key (workspace_id) references workspace
alter table document add constraint document_workspace_fk foreign key (workspace_id) references workspace
alter table expense add constraint expense_category_fk foreign key (category_id) references category
alter table expense_attachments add constraint expense_attachments_document_fk foreign key (document_id) references document
alter table expense_attachments add constraint expense_attachments_expense_fk foreign key (expense_id) references expense
alter table income add constraint income_category_fk foreign key (category_id) references category
alter table income_attachments add constraint income_attachments_document_fk foreign key (document_id) references document
alter table income_attachments add constraint income_attachments_income_fk foreign key (income_id) references income
alter table invoice add constraint invoice_customer_fk foreign key (customer_id) references customer
alter table invoice add constraint invoice_income_fk foreign key (income_id) references income
alter table invoice_attachments add constraint invoice_attachments_document_fk foreign key (document_id) references document
alter table invoice_attachments add constraint invoice_attachments_invoice_fk foreign key (invoice_id) references invoice
alter table tax_payment_attachments add constraint tax_payment_attachments_document_fk foreign key (document_id) references document
alter table tax_payment_attachments add constraint tax_payment_attachments_tax_payment_fk foreign key (tax_payment_id) references tax_payment
alter table tax_payment add constraint tax_payment_workspace_fk foreign key (workspace_id) references workspace
alter table workspace add constraint workspace_owner_fk foreign key (owner_id) references platform_user