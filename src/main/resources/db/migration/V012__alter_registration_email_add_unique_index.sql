alter table registration_email
    add constraint registration_email_email_key unique (registration_id, email);