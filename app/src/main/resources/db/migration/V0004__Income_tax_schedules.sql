create table income_tax_schedule (
    id character varying(10) not null,
    version integer not null,
    created_at timestamp with time zone not null,
    country_code character varying(2) not null,
    jurisdiction character varying(255) not null,
    taxpayer character varying(255) not null,
    tax_period_label character varying(100) not null,
    period_start date not null,
    period_end date not null,
    currency character varying(3) not null,
    basis character varying(50) not null,
    limitations text not null,
    primary key (id),
    constraint income_tax_schedule_period_check check (period_start <= period_end),
    constraint income_tax_schedule_country_period_uq unique (country_code, jurisdiction, taxpayer, period_start)
);

create table income_tax_bracket (
    schedule_id character varying(10) not null,
    threshold numeric(18, 2) not null,
    rate numeric(7, 6) not null,
    primary key (schedule_id, threshold),
    constraint income_tax_bracket_schedule_fk foreign key (schedule_id) references income_tax_schedule(id) on delete cascade,
    constraint income_tax_bracket_threshold_check check (threshold >= 0),
    constraint income_tax_bracket_rate_check check (rate >= 0 and rate <= 1)
);

create table income_tax_schedule_source (
    schedule_id character varying(10) not null,
    title text not null,
    url character varying(2048) not null,
    accessed_on date not null,
    primary key (schedule_id, url),
    constraint income_tax_schedule_source_schedule_fk foreign key (schedule_id) references income_tax_schedule(id) on delete cascade
);
