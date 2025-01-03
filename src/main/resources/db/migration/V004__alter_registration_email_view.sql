drop view registration_email_view;

create view registration_email_view as
select r.id  as registration_id,
       r.year,
       re.id as registration_email_id,
       re.email,
       re.link,
       re.sent_at
from registration_email re
         join registration r on re.registration_id = r.id;
