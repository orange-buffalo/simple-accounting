alter table account_statement_record add column party_name text;

-- [jooq ignore start]
update account_statement_record
set party_name = coalesce(
    nullif(btrim(substring(additional_data from '(?m)^Sender: ([^\r\n]+)')), ''),
    nullif(btrim(substring(additional_data from '(?m)^Merchant: ([^\r\n]+)')), '')
)
where provider = 'WISE';
-- [jooq ignore stop]
