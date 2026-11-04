alter table wise_integration_settings add column fetch_monthly_statements boolean;
update wise_integration_settings set fetch_monthly_statements = true;
alter table wise_integration_settings alter column fetch_monthly_statements set not null;
