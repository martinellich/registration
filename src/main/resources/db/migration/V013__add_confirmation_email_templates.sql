alter table registration
    add column confirmation_email_subject_new varchar,
    add column confirmation_email_text_new text,
    add column confirmation_email_subject_update varchar,
    add column confirmation_email_text_update text;

-- Set default templates in German
update registration
set confirmation_email_subject_new = 'Anmeldebestätigung',
    confirmation_email_text_new = 'Vielen Dank für deine Anmeldung!

Folgende Personen wurden angemeldet:
%PERSON_NAMES%

Angemeldete Anlässe:
%EVENTS%

Anmeldung ist gültig von %OPEN_FROM% bis %OPEN_UNTIL%.

%REMARKS%

Du kannst deine Anmeldung jederzeit ändern unter: %LINK%',
    confirmation_email_subject_update = 'Anmeldung aktualisiert',
    confirmation_email_text_update = 'Deine Anmeldung wurde erfolgreich aktualisiert!

Folgende Personen sind angemeldet:
%PERSON_NAMES%

Aktualisierte Anlässe:
%EVENTS%

Anmeldung ist gültig von %OPEN_FROM% bis %OPEN_UNTIL%.

%REMARKS%

Du kannst deine Anmeldung jederzeit ändern unter: %LINK%'
where confirmation_email_subject_new is null;
