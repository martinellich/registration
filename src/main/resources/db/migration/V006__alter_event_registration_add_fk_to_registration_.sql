alter table event_registration
    add foreign key (registration_id) references registration (id) on delete cascade;