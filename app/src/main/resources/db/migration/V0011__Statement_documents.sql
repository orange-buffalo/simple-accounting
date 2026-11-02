alter table account_statement_record add column title text;

-- [jooq ignore start]
update account_statement_record
set title = substring(additional_data from '(?n)^Description: ([^\r\n]+)')
where provider = 'WISE' and type = 'CONVERSION'
  and substring(additional_data from '(?n)^Description: ([^\r\n]+)') ~ '[^[:space:]]';
-- [jooq ignore stop]

create table account_statement_attachments (
    account_statement_id varchar(10) not null references account_statement_record(id) on delete cascade,
    document_id varchar(10) not null references document(id),
    primary key (account_statement_id, document_id)
);

create index account_statement_attachments_document_idx on account_statement_attachments(document_id);
