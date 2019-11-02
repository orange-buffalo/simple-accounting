update platform_user set is_admin = false where is_admin is null;

alter table platform_user
    alter column is_admin set not null;
