insert into person(id,last_name,first_name,email,date_of_birth,active) values (1,'Lane','Eula','simon.martinelli@gmail.com','2018-01-12', true);
insert into person(id,last_name,first_name,email,date_of_birth,active) values (2,'Rodriquez','Barry','barry.rodriquez@zun.mm','2013-12-07', true);
insert into person(id,last_name,first_name,email,date_of_birth,active) values (3,'Selvi','Eugenia','eugenia.selvi@capfad.vn','2019-12-05', true);
insert into person(id,last_name,first_name,email,date_of_birth,active) values (4,'Miles','Alejandro','alejandro.miles@dec.bn','2013-12-11', false);
insert into person(id,last_name,first_name,email,date_of_birth,active) values (5,'Tesi','Cora','cora.tesi@bivo.yt','2019-10-04', true);
insert into person(id,last_name,first_name,email,date_of_birth,active) values (6,'Ishii','Marguerite','marguerite.ishii@judbilo.gn','2016-04-30', true);
insert into person(id,last_name,first_name,email,date_of_birth,active) values (7,'Jacobs','Mildred','mildred.jacobs@joraf.wf','2019-04-16', false);
insert into person(id,last_name,first_name,email,date_of_birth,active) values (8,'Goodman','Gene','gene.goodman@kem.tl','2013-07-30', true);
insert into person(id,last_name,first_name,email,date_of_birth,active) values (9,'Bennett','Lettie','lettie.bennett@odeter.bb','2018-06-30', true);
insert into person(id,last_name,first_name,email,date_of_birth,active) values (10,'Leach','Mabel','mabel.leach@lisohuje.vi','2017-03-09', true);
insert into person(id,last_name,first_name,email,date_of_birth,active) values (11,'Miccinesi','Jordan','jordan.miccinesi@duod.gy','2020-10-19', true);
insert into person(id,last_name,first_name,email,date_of_birth,active) values (12,'Parkes','Marie','marie.parkes@nowufpus.ph','2016-11-18', true);

insert into event(id,title,description,location,from_date,to_date,mandatory) values ( 1,'CIS 2023','Concours Inter Section','Erlach', '2023-05-21',null,false);
insert into event(id,title,description,location,from_date,to_date,mandatory) values ( 2,'Jugendmeisterschaft 2023','','Erlach', '2023-09-01',null,false);
insert into event(id,title,description,location,from_date,to_date,mandatory) values ( 3,'CIS 2024','Concours Inter Section','Erlach', '2024-05-24',null,false);
insert into event(id,title,description,location,from_date,to_date,mandatory) values ( 4,'CIS 2025','Concours Inter Section','Twann', '2025-05-12',null,true);
insert into event(id,title,description,location,from_date,to_date,mandatory) values ( 5,'Jugendmeisterschaft 2025','','Erlach', '2025-08-31',null,false);

insert into registration(id,title,year,open_from,open_until,remarks,email_text,confirmation_email_subject_new,confirmation_email_text_new,confirmation_email_subject_update,confirmation_email_text_update)
values (1,'Anmeldung',2023,'2023-01-01','2023-02-28','Some remarks','Mail text %s','Registration Confirmed','Thank you for registering!

Persons:
%PERSON_NAMES%

Events:
%EVENTS%

Registration period: %OPEN_FROM% to %OPEN_UNTIL%

%REMARKS%

You can update your registration at: %LINK%','Registration Updated','Your registration has been updated!

Persons:
%PERSON_NAMES%

Events:
%EVENTS%

Registration period: %OPEN_FROM% to %OPEN_UNTIL%

%REMARKS%

You can update your registration at: %LINK%');

insert into registration(id,title,year,open_from,open_until,remarks,email_text,confirmation_email_subject_new,confirmation_email_text_new,confirmation_email_subject_update,confirmation_email_text_update)
values (2,'Anmeldung',2024,'2024-01-01','2024-02-28','Some remarks','Mail text %s','Registration Confirmed','Confirmation for %PERSON_NAMES%','Registration Updated','Update for %PERSON_NAMES%');

insert into registration(id,title,year,open_from,open_until,remarks,email_text,confirmation_email_subject_new,confirmation_email_text_new,confirmation_email_subject_update,confirmation_email_text_update)
values (3,'Anmeldung',2025,'2025-01-01','2099-12-31','Open registration for testing','Mail text %s','Registration Confirmed','Thank you!','Registration Updated','Updated!');

insert into registration_person (registration_id,person_id) values (1,1);
insert into registration_person (registration_id,person_id) values (1,5);

insert into registration_event (registration_id,event_id) values (1,1);
insert into registration_event (registration_id,event_id) values (1,2);

insert into event_registration (registration_id,event_id,person_id,registered) values (1,1,1, true);
insert into event_registration (registration_id,event_id,person_id,registered) values (1,1,2, false);

insert into registration_person (registration_id,person_id) values (3,2);

insert into registration_event (registration_id,event_id) values (3,4);
insert into registration_event (registration_id,event_id) values (3,5);

insert into registration_email (id,registration_id,email,link,sent_at) values (1,1,'jordan.miccinesi@duod.gy','550e8400e29b41d4a716446655440000','2023-01-01 11:00:00');
insert into registration_email (id,registration_id,email,link,sent_at) values (2,1,'cora.tesi@bivo.yt','2226914588a24213a631dcdd475f81b6',null);
insert into registration_email (id,registration_id,email,link,sent_at) values (3,3,'barry.rodriquez@zun.mm','openregistrationlink123456789',null);
insert into registration_email (id,registration_id,email,link,sent_at) values (4,3,'eugene.selvi@test.com','testlink4567890123456789abcd',null);
insert into registration_email (id,registration_id,email,link,sent_at) values (5,3,'mabel.leach@test.com','testlink5567890123456789abcd',null);
insert into registration_email_person (registration_email_id,person_id) values (1,1);
insert into registration_email_person (registration_email_id,person_id) values (2,5);
insert into registration_email_person (registration_email_id,person_id) values (3,2);
insert into registration_email_person (registration_email_id,person_id) values (4,2);
insert into registration_email_person (registration_email_id,person_id) values (5,2);
