alter table wise_integration_settings
    add column fetch_transactions_periodically boolean not null default true,
    add column last_successful_fetch_time timestamp with time zone,
    add column authentication_required boolean not null default false;

alter table wise_integration_account
    add column last_successful_fetch_time timestamp with time zone;

create table account_statement_record (
    id character varying(10) not null primary key,
    version integer not null,
    created_at timestamp with time zone not null,
    workspace_id character varying(10) not null references workspace(id),
    provider character varying not null,
    integration_id character varying(10) not null,
    profile_id character varying not null,
    account_id character varying not null,
    account_currency character varying(3) not null,
    external_id character varying not null,
    direction character varying not null,
    occurred_at timestamp with time zone not null,
    amount_in_cents bigint not null,
    currency character varying(3) not null,
    fees_in_cents bigint,
    fees_currency character varying(3),
    running_balance_in_cents bigint,
    running_balance_currency character varying(3),
    transaction_type text,
    description text,
    payment_reference text,
    counterparty_name text,
    counterparty_account text,
    merchant_category text,
    merchant_address text,
    original_amount_in_cents bigint,
    original_currency character varying(3),
    source_amount_in_cents bigint,
    source_currency character varying(3),
    target_amount_in_cents bigint,
    target_currency character varying(3),
    exchange_rate numeric,
    constraint account_statement_external_uq unique
        (provider, integration_id, profile_id, account_id, account_currency, external_id, direction)
);

create index account_statement_workspace_date_idx on account_statement_record(workspace_id, occurred_at);
