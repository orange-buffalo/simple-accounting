/* [jooq ignore start] */
alter table income
alter column id bigint generated always as identity not null;

/* [jooq ignore stop] */
