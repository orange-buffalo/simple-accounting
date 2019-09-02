alter table platform_user
    add column documents_storage varchar(255);

-- noinspection SqlWithoutWhere
update platform_user
set documents_storage = 'google-drive';
