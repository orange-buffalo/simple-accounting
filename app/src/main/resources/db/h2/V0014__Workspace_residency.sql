alter table workspace add column residency varchar(2);
update workspace set residency = 'AU';
alter table workspace alter column residency set not null;
