alter table expense
  add column title varchar(255);

update expense e
set e.title = (select c.name from category c where c.id = e.category_id);

alter table expense
  alter column title set not null;
