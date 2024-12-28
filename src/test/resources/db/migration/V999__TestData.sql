insert into security_user (id,email,first_name,last_name,secret) values (1, 'john.normal@acme.com','John', 'Normal','$2a$10$xdbKoM48VySZqVSU/cSlVeJn0Z04XCZ7KZBjUBC00eKo5uLswyOpe');
insert into security_user (id,email,first_name,last_name,secret) values (2,'emma.executive@acme.com','Emma', 'Executive','$2a$10$jpLNVNeA7Ar/ZQ2DKbKCm.MuT2ESe.Qop96jipKMq7RaUgCoQedV.');
insert into security_group (id,name) values ('1', 'USER');
insert into security_group (id,name) values ('2', 'ADMIN');
insert into user_group (user_id,group_id) values ('1', '2');
insert into user_group (user_id,group_id) values ('2', '1');
insert into user_group (user_id,group_id) values ('2', '2');

insert into person(id,last_name,first_name,email,date_of_birth) values (1,'Lane','Eula','eula.lane@jigrormo.ye','2018-01-12');
insert into person(id,last_name,first_name,email,date_of_birth) values (2,'Rodriquez','Barry','barry.rodriquez@zun.mm','2023-12-07');
insert into person(id,last_name,first_name,email,date_of_birth) values (3,'Selvi','Eugenia','eugenia.selvi@capfad.vn','2019-12-05');
insert into person(id,last_name,first_name,email,date_of_birth) values (4,'Miles','Alejandro','alejandro.miles@dec.bn','2023-12-11');
insert into person(id,last_name,first_name,email,date_of_birth) values (5,'Tesi','Cora','cora.tesi@bivo.yt','2019-10-04');
insert into person(id,last_name,first_name,email,date_of_birth) values (6,'Ishii','Marguerite','marguerite.ishii@judbilo.gn','2016-04-30');
insert into person(id,last_name,first_name,email,date_of_birth) values (7,'Jacobs','Mildred','mildred.jacobs@joraf.wf','2019-04-16');
insert into person(id,last_name,first_name,email,date_of_birth) values (8,'Goodman','Gene','gene.goodman@kem.tl','2023-07-30');
insert into person(id,last_name,first_name,email,date_of_birth) values (9,'Bennett','Lettie','lettie.bennett@odeter.bb','2018-06-30');
insert into person(id,last_name,first_name,email,date_of_birth) values (10,'Leach','Mabel','mabel.leach@lisohuje.vi','2017-03-09');
insert into person(id,last_name,first_name,email,date_of_birth) values (11,'Miccinesi','Jordan','jordan.miccinesi@duod.gy','2020-10-19');
insert into person(id,last_name,first_name,email,date_of_birth) values (12,'Parkes','Marie','marie.parkes@nowufpus.ph','2016-11-18');

insert into event(id,title,description,location,from_date,to_date) values ( 1,'jigrormo','zun capfad','Bern', '1955-05-24','1955-05-24');
insert into event(id,title,description,location,from_date,to_date) values (2,'zun','capfad dec','Aarau', '2014-05-24','2014-05-24');
insert into event(id,title,description,location,from_date,to_date) values (3,'capfad','dec bivo','Zürich', '1974-05-09','1974-05-09');
insert into event(id,title,description,location,from_date,to_date) values (4,'dec','bivo judbilo','Lausanne', '2014-06-26','2014-06-26');
insert into event(id,title,description,location,from_date,to_date) values (5,'bivo','judbilo joraf','Lugano', '1972-08-23','1972-08-23');
insert into event(id,title,description,location,from_date,to_date) values (6,'judbilo','joraf kem','Chur', '1938-05-21','1938-05-21');
