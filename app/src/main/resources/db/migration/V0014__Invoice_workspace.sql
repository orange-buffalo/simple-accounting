alter table invoice add column workspace_id character varying(10);

update invoice
set workspace_id = customer.workspace_id
from customer
where customer.id = invoice.customer_id;

alter table invoice alter column workspace_id set not null;
alter table invoice add constraint invoice_workspace_fk foreign key (workspace_id) references workspace(id);

create index invoice_workspace_idx on invoice(workspace_id);
