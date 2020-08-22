alter table invoice
    add column status varchar2(30);

update invoice
set status = case
                 when date_cancelled is not null then 'CANCELLED'
                 when date_paid is not null then 'PAID'
                 when due_date < now() then 'OVERDUE'
                 when date_sent is not null then 'SENT'
                 else 'DRAFT'
    end;

alter table invoice
    alter column status set not null;

alter table invoice
    alter column date_cancelled set data type timestamp;

alter table invoice
    alter column date_cancelled rename to time_cancelled;
