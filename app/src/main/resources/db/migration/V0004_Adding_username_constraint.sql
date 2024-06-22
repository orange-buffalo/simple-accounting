alter table platform_user
    add constraint platform_user_user_name_uq unique (user_name);
