insert into security_user (id,email,first_name,last_name,secret) values (1, 'jugi@tverlach.ch','Nadine', 'Didier','$2a$10$KV6tLxszp.zVX0UeIC2KhO7ipj6jWd8krVU7HaUpKPRunk.CcF5bG');
insert into security_user (id,email,first_name,last_name,secret) values (2,'simon@martinelli.ch','Simon', 'Martinelli','$2a$10$/wgKpLLOOrRdtMe6QXGNH.WWIMahg1j0uTnI8spvKmhaQTpvHMKQC');

insert into security_group (id,name) values ('1', 'USER');
insert into security_group (id,name) values ('2', 'ADMIN');

insert into user_group (user_id,group_id) values ('1', '2');
insert into user_group (user_id,group_id) values ('2', '1');
insert into user_group (user_id,group_id) values ('2', '2');