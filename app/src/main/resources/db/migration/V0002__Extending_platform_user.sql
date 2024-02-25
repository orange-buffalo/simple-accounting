alter table platform_user
    add column activated boolean;

update platform_user
set activated = true;

alter table platform_user
    alter column activated set not null;
