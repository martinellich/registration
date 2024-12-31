insert into security_user (id,email,first_name,last_name,secret) values (1, 'jugi@tverlach.ch','Nadine', 'Didier','$2a$10$xdbKoM48VySZqVSU/cSlVeJn0Z04XCZ7KZBjUBC00eKo5uLswyOpe');
insert into security_user (id,email,first_name,last_name,secret) values (2,'simon@martinelli.ch','Simon', 'Martinelli','$2a$10$jpLNVNeA7Ar/ZQ2DKbKCm.MuT2ESe.Qop96jipKMq7RaUgCoQedV.');
insert into security_group (id,name) values ('1', 'USER');
insert into security_group (id,name) values ('2', 'ADMIN');
insert into user_group (user_id,group_id) values ('1', '2');
insert into user_group (user_id,group_id) values ('2', '1');
insert into user_group (user_id,group_id) values ('2', '2');

insert into person(id,last_name,first_name,email,date_of_birth,active) values (1,'Lane','Eula','eula.lane@jigrormo.ye','2018-01-12', true);
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

insert into event(id,title,description,location,from_date,to_date) values ( 1,'CIS 2024','Concours Inter Section','Erlach', '2024-05-24',null);
insert into event(id,title,description,location,from_date,to_date) values ( 2,'CIS 2025','Concours Inter Section','Twann', '2025-05-12',null);
insert into event(id,title,description,location,from_date,to_date) values ( 3,'Jugendmeisterschaft 2025','','Erlach', '2025-08-31',null);

insert into registration(id,year,open_from,open_until) values (1,2023,'2023-01-01','2023-02-28');
insert into registration(id,year,open_from,open_until) values (2,2024,'2024-01-01','2024-02-28');

insert into registration_person (registration_id,person_id) values (1,1);
insert into registration_person (registration_id,person_id) values (1,5);

insert into registration_event (registration_id,event_id) values (1,2);
insert into registration_event (registration_id,event_id) values (1,3);

insert into event_registration (event_id,person_id) values (2,1);

insert into registration_email (id,registration_id,email,link) values (1,1,'jordan.miccinesi@duod.gy','550e8400e29b41d4a716446655440000');
insert into registration_email_person (registration_email_id,person_id) values (1,1);
