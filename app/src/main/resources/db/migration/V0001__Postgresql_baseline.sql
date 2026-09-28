create table platform_user (
    id character varying(10) not null,
    version integer not null,
    user_name character varying(255) not null,
    password_hash character varying(255) not null,
    is_admin boolean not null,
    documents_storage character varying(255),
    failed_attempts_count integer not null,
    temporary_lock_expiration_time timestamp with time zone,
    language character varying(36) not null,
    locale character varying(36) not null,
    activated boolean not null,
    created_at timestamp with time zone not null,
    primary key (id),
    constraint platform_user_user_name_uq unique (user_name)
);

create table persistent_oauth2_authorized_client (
    id character varying(10) not null,
    version integer not null,
    access_token character varying(2000) not null,
    access_token_expires_at timestamp with time zone,
    access_token_issued_at timestamp with time zone,
    client_registration_id character varying(255) not null,
    refresh_token character varying(2000),
    refresh_token_issued_at timestamp with time zone,
    user_name character varying(255) not null,
    created_at timestamp with time zone not null,
    primary key (id)
);

create table oauth_provider (
    id character varying(10) not null,
    version integer not null,
    created_at timestamp with time zone not null,
    name character varying(255) not null,
    client_id character varying(255) not null,
    client_secret character varying(255) not null,
    authorization_url character varying(2048) not null,
    token_url character varying(2048) not null,
    user_info_url character varying(2048) not null,
    user_id_attribute character varying(255) not null,
    primary key (id),
    constraint oauth_provider_name_uq unique (name)
);

create table persistent_oauth2_authorized_client_access_token_scopes (
    client_id character varying(10) not null,
    access_token_scopes character varying(255),
    constraint pauth2ac_access_token_scopes_scopes_client_fk foreign key(client_id) references persistent_oauth2_authorized_client(id) on delete cascade
);

create table workspace (
    id character varying(10) not null,
    version integer not null,
    name character varying(255) not null,
    owner_id character varying(10) not null,
    default_currency character varying(3) not null,
    created_at timestamp with time zone not null,
    primary key (id),
    constraint workspace_owner_fk foreign key(owner_id) references platform_user(id)
);

create table refresh_token (
    id character varying(10) not null,
    version integer not null,
    expiration_time timestamp with time zone not null,
    token character varying(2048) not null,
    user_id character varying(10) not null,
    created_at timestamp with time zone not null,
    primary key (id),
    constraint refresh_token_user_fk foreign key(user_id) references platform_user(id)
);

create table google_drive_storage_integration (
    id character varying(10) not null,
    version integer not null,
    folder_id character varying(255),
    user_id character varying(10) not null,
    created_at timestamp with time zone not null,
    primary key (id),
    constraint google_drive_storage_integration_uq unique (user_id),
    constraint gdrive_storage_integration_user_fk foreign key(user_id) references platform_user(id)
);

create table user_activation_token (
    id character varying(10) not null,
    version integer not null,
    user_id character varying(10) not null,
    token character varying(255) not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    constraint user_activation_token_user_uq unique (user_id),
    constraint user_activation_token_uq unique (token),
    constraint user_activation_token_user_fk foreign key(user_id) references platform_user(id)
);

create table documents_migration (
    id character varying(10) not null,
    version integer not null,
    created_at timestamp with time zone not null,
    completed_at timestamp with time zone,
    user_id character varying(10) not null,
    migrated_documents_count integer not null,
    primary key (id),
    constraint documents_migration_user_fk foreign key(user_id) references platform_user(id)
);

create table oauth_provider_scope (
    provider_id character varying(10) not null,
    scope character varying(255) not null,
    primary key (provider_id, scope),
    constraint oauth_provider_scope_provider_fk foreign key(provider_id) references oauth_provider(id) on delete cascade
);

create table user_oauth_identity (
    id character varying(10) not null,
    version integer not null,
    created_at timestamp with time zone not null,
    user_id character varying(10) not null,
    provider_id character varying(10) not null,
    external_id character varying(255) not null,
    primary key (id),
    constraint user_oauth_identity_external_id_uq unique (provider_id, external_id),
    constraint user_oauth_identity_user_provider_uq unique (user_id, provider_id),
    constraint user_oauth_identity_user_fk foreign key(user_id) references platform_user(id),
    constraint user_oauth_identity_provider_fk foreign key(provider_id) references oauth_provider(id)
);

create table oauth_authentication_request (
    id character varying(10) not null,
    version integer not null,
    created_at timestamp with time zone not null,
    state character varying(255) not null,
    browser_binding character varying(255) not null,
    provider_id character varying(10) not null,
    user_id character varying(10) not null,
    purpose character varying(20) not null,
    issue_refresh_token_cookie boolean not null,
    expires_at timestamp with time zone not null,
    primary key (id),
    constraint oauth_authentication_request_state_uq unique (state),
    constraint oauth_authentication_request_user_fk foreign key(user_id) references platform_user(id),
    constraint oauth_authentication_request_provider_fk foreign key(provider_id) references oauth_provider(id)
);

create table customer (
    id character varying(10) not null,
    version integer not null,
    name character varying(255) not null,
    workspace_id character varying(10) not null,
    created_at timestamp with time zone not null,
    primary key (id),
    constraint customer_workspace_fk foreign key(workspace_id) references workspace(id)
);

create table category (
    id character varying(10) not null,
    version integer not null,
    description character varying(1024),
    expense boolean not null,
    income boolean not null,
    name character varying(255) not null,
    workspace_id character varying(10) not null,
    created_at timestamp with time zone not null,
    primary key (id),
    constraint category_workspace_fk foreign key(workspace_id) references workspace(id)
);

create table document (
    id character varying(10) not null,
    version integer not null,
    time_uploaded timestamp with time zone not null,
    name character varying(255) not null,
    storage_id character varying(255) not null,
    storage_location character varying(2048),
    workspace_id character varying(10) not null,
    size_in_bytes bigint,
    created_at timestamp with time zone not null,
    mime_type character varying(255) not null,
    primary key (id),
    constraint document_workspace_fk foreign key(workspace_id) references workspace(id)
);

create table general_tax (
    id character varying(10) not null,
    version integer not null,
    title character varying(255) not null,
    description character varying(255),
    rate_in_bps integer not null,
    workspace_id character varying(10) not null,
    created_at timestamp with time zone not null,
    primary key (id),
    constraint general_tax_workspace_fk foreign key(workspace_id) references workspace(id)
);

create table income_tax_payment (
    id character varying(10) not null,
    version integer not null,
    amount bigint not null,
    date_paid date not null,
    notes character varying(1024),
    workspace_id character varying(10) not null,
    title character varying(255) not null,
    reporting_date date not null,
    created_at timestamp with time zone not null,
    primary key (id),
    constraint income_tax_payment_workspace_fk foreign key(workspace_id) references workspace(id)
);

create table workspace_access_token (
    id character varying(10) not null,
    version integer not null,
    revoked boolean not null,
    time_created timestamp with time zone not null,
    token character varying(255) not null,
    valid_till timestamp with time zone not null,
    workspace_id character varying(10) not null,
    created_at timestamp with time zone not null,
    primary key (id),
    constraint workspace_access_token_token_uq unique (token),
    constraint workspace_access_token_workspace_fk foreign key(workspace_id) references workspace(id)
);

create table standalone_document (
    id character varying(10) not null,
    version integer not null,
    created_at timestamp with time zone not null,
    title character varying(255) not null,
    document_id character varying(10) not null,
    primary key (id),
    constraint standalone_document_document_fk foreign key(document_id) references document(id)
);

create table invoice (
    id character varying(10) not null,
    version integer not null,
    customer_id character varying(10) not null,
    title character varying(255) not null,
    date_issued date not null,
    date_sent date,
    date_paid date,
    time_cancelled timestamp with time zone,
    due_date date not null,
    currency character varying(3) not null,
    amount bigint not null,
    notes character varying(1024),
    general_tax_id character varying(10),
    status character varying(30) not null,
    created_at timestamp with time zone not null,
    primary key (id),
    constraint invoice_general_tax_fk foreign key(general_tax_id) references general_tax(id),
    constraint invoice_customer_fk foreign key(customer_id) references customer(id)
);

create table expense (
    id character varying(10) not null,
    version integer not null,
    income_taxable_original_amount_in_default_currency bigint,
    converted_original_amount_in_default_currency bigint,
    currency character varying(3) not null,
    date_paid date not null,
    notes character varying(1024),
    original_amount bigint not null,
    percent_on_business integer not null,
    category_id character varying(10),
    income_taxable_adjusted_amount_in_default_currency bigint,
    title character varying(255) not null,
    workspace_id character varying(10) not null,
    general_tax_id character varying(10),
    general_tax_amount bigint,
    general_tax_rate_in_bps integer,
    use_different_exchange_rate_for_income_tax_purposes boolean not null,
    status character varying(255) not null,
    converted_adjusted_amount_in_default_currency bigint,
    created_at timestamp with time zone not null,
    primary key (id),
    constraint expense_general_tax_fk foreign key(general_tax_id) references general_tax(id),
    constraint expense_workspace_fk foreign key(workspace_id) references workspace(id),
    constraint expense_category_fk foreign key(category_id) references category(id)
);

create table saved_workspace_access_token (
    id character varying(10) not null,
    version integer not null,
    owner_id character varying(10) not null,
    workspace_access_token_id character varying(10) not null,
    created_at timestamp with time zone not null,
    primary key (id),
    constraint saved_ws_access_token_owner_fk foreign key(owner_id) references platform_user(id),
    constraint saved_ws_access_token_ws_access_token_fk foreign key(workspace_access_token_id) references workspace_access_token(id)
);

create table income_tax_payment_attachments (
    income_tax_payment_id character varying(10) not null,
    document_id character varying(10) not null,
    primary key (income_tax_payment_id, document_id),
    constraint income_tax_payment_attachments_document_fk foreign key(document_id) references document(id),
    constraint income_tax_payment_attachments_tax_payment_fk foreign key(income_tax_payment_id) references income_tax_payment(id)
);

create table documents_migration_document (
    migration_id character varying(10) not null,
    document_id character varying(10) not null,
    primary key (migration_id, document_id),
    constraint documents_migration_document_document_fk foreign key(document_id) references document(id),
    constraint documents_migration_document_migration_fk foreign key(migration_id) references documents_migration(id)
);

create table income (
    id character varying(10) not null,
    version integer not null,
    converted_original_amount_in_default_currency bigint,
    currency character varying(3) not null,
    date_received date not null,
    notes character varying(1024),
    original_amount bigint not null,
    income_taxable_original_amount_in_default_currency bigint,
    category_id character varying(10),
    title character varying(255) not null,
    workspace_id character varying(10) not null,
    general_tax_id character varying(10),
    general_tax_amount bigint,
    general_tax_rate_in_bps integer,
    use_different_exchange_rate_for_income_tax_purposes boolean not null,
    status character varying(255) not null,
    income_taxable_adjusted_amount_in_default_currency bigint,
    converted_adjusted_amount_in_default_currency bigint,
    linked_invoice_id character varying(10),
    created_at timestamp with time zone not null,
    primary key (id),
    constraint income_linked_invoice_fk foreign key(linked_invoice_id) references invoice(id),
    constraint income_general_tax_fk foreign key(general_tax_id) references general_tax(id),
    constraint income_workspace_fk foreign key(workspace_id) references workspace(id),
    constraint income_category_fk foreign key(category_id) references category(id)
);

create table expense_attachments (
    expense_id character varying(10) not null,
    document_id character varying(10) not null,
    primary key (expense_id, document_id),
    constraint expense_attachments_document_fk foreign key(document_id) references document(id),
    constraint expense_attachments_expense_fk foreign key(expense_id) references expense(id)
);

create table invoice_attachments (
    invoice_id character varying(10) not null,
    document_id character varying(10) not null,
    primary key (invoice_id, document_id),
    constraint invoice_attachments_invoice_fk foreign key(invoice_id) references invoice(id),
    constraint invoice_attachments_document_fk foreign key(document_id) references document(id)
);

create table income_attachments (
    income_id character varying(10) not null,
    document_id character varying(10) not null,
    primary key (income_id, document_id),
    constraint income_attachments_document_fk foreign key(document_id) references document(id),
    constraint income_attachments_income_fk foreign key(income_id) references income(id)
);

create table sa_h2_import (completed_at timestamp with time zone not null);
