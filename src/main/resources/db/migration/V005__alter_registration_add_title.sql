alter table registration
    add column title varchar;

drop view registration_view;

create view registration_view as
select r.id,
       r.title,
       r.year,
       r.open_from,
       r.open_until,
       (select count(*)
        from registration_email
        where registration_id = r.id) as email_created_count,
       (select count(*)
        from registration_email
        where registration_id = r.id
          and sent_at is not null)    as email_sent_count
from registration r;