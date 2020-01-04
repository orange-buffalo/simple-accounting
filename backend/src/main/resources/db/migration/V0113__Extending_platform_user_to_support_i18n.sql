alter table platform_user
    add column language varchar(36);

update platform_user
set language = 'en';

alter table platform_user
    alter column language set not null;

alter table platform_user
    add column locale varchar(36);

update platform_user
set locale = 'en_AU';

alter table platform_user
    alter column locale set not null;
