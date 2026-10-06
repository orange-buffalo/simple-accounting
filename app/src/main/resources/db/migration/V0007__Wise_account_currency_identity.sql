create table wise_integration_account_with_currency (
    settings_id varchar(10) not null references wise_integration_settings(id) on delete cascade,
    profile_id bigint not null,
    account_id bigint not null,
    currency varchar(3) not null,
    primary key (settings_id, profile_id, account_id, currency)
);

insert into wise_integration_account_with_currency (settings_id, profile_id, account_id, currency)
select settings_id, profile_id, account_id, currency from wise_integration_account;

drop table wise_integration_account;

alter table wise_integration_account_with_currency rename to wise_integration_account;
