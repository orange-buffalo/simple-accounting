alter table platform_user
    add column failed_attempts_count integer;

update platform_user
set failed_attempts_count = 0;

alter table platform_user
    alter column failed_attempts_count set not null;

alter table platform_user
    add column temporary_lock_expiration_time timestamp;
