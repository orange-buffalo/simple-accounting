alter table platform_user add column time_zone varchar(255);
update platform_user set time_zone = 'Australia/Melbourne';
alter table platform_user alter column time_zone set not null;
