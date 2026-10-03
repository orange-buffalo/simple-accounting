alter table income_tax_schedule rename column period_end_exclusive to period_end;
alter table income_tax_schedule drop constraint income_tax_schedule_period_check;

update income_tax_schedule set period_end = period_end - 1;

alter table income_tax_schedule add constraint income_tax_schedule_period_check check (period_start <= period_end);
