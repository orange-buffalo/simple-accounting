/* [jooq ignore start] */

alter table income
alter column id bigint generated by default as identity not null;

alter table invoice
alter column id bigint generated by default as identity not null;

alter table expense
alter column id bigint generated by default as identity not null;

alter table income_tax_payment
alter column id bigint generated by default as identity not null;

alter table document
alter column id bigint generated by default as identity not null;

alter table category
alter column id bigint generated by default as identity not null;

/* [jooq ignore stop] */
