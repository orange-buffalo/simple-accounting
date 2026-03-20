alter table "DOCUMENT"
    add column "MIME_TYPE" character varying(255);

update "DOCUMENT"
set "MIME_TYPE" = case
    when lower("NAME") like '%.pdf' then 'application/pdf'
    when lower("NAME") like '%.txt' then 'text/plain'
    when lower("NAME") like '%.jpg' then 'image/jpeg'
    when lower("NAME") like '%.jpeg' then 'image/jpeg'
    when lower("NAME") like '%.png' then 'image/png'
    when lower("NAME") like '%.gif' then 'image/gif'
    when lower("NAME") like '%.bmp' then 'image/bmp'
    when lower("NAME") like '%.svg' then 'image/svg+xml'
    when lower("NAME") like '%.webp' then 'image/webp'
    when lower("NAME") like '%.tiff' then 'image/tiff'
    when lower("NAME") like '%.tif' then 'image/tiff'
    when lower("NAME") like '%.doc' then 'application/msword'
    when lower("NAME") like '%.docx' then 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    when lower("NAME") like '%.xls' then 'application/vnd.ms-excel'
    when lower("NAME") like '%.xlsx' then 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    when lower("NAME") like '%.ppt' then 'application/vnd.ms-powerpoint'
    when lower("NAME") like '%.pptx' then 'application/vnd.openxmlformats-officedocument.presentationml.presentation'
    when lower("NAME") like '%.csv' then 'text/csv'
    when lower("NAME") like '%.zip' then 'application/zip'
    when lower("NAME") like '%.html' then 'text/html'
    when lower("NAME") like '%.htm' then 'text/html'
    else 'application/octet-stream'
end;

alter table "DOCUMENT"
    alter column "MIME_TYPE" set not null;
