alter table wise_integration_settings
    add column fetch_transactions_periodically boolean not null default true,
    add column last_successful_fetch_time timestamp with time zone,
    add column authentication_required boolean not null default false;

alter table wise_integration_settings
    alter column fetch_transactions_periodically drop default;

alter table wise_integration_settings
    alter column authentication_required drop default;

alter table wise_integration_account
    add column last_successful_fetch_time timestamp with time zone;

create table account_statement_record (
    id character varying(10) not null primary key,
    version integer not null,
    created_at timestamp with time zone not null,
    workspace_id character varying(10) not null references workspace(id),
    provider character varying not null,
    integration_id character varying(10) not null,
    account_id character varying not null,
    account_currency character varying(3) not null,
    external_id character varying not null,
    type character varying not null,
    occurred_at timestamp with time zone not null,
    amount_in_cents bigint not null check (amount_in_cents >= 0),
    currency character varying(3) not null,
    fees_in_cents bigint,
    fees_currency character varying(3),
    running_balance_in_cents bigint,
    running_balance_currency character varying(3),
    additional_data text,
    original_amount_in_cents bigint,
    original_currency character varying(3),
    source_amount_in_cents bigint,
    source_currency character varying(3),
    target_amount_in_cents bigint,
    target_currency character varying(3),
    exchange_rate numeric,
    constraint account_statement_external_uq unique
        (integration_id, account_id, external_id)
);

create index account_statement_workspace_date_idx on account_statement_record(workspace_id, occurred_at);
