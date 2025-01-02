create sequence person_seq start with 1000;

create table person
(
    id            bigint  not null primary key default nextval('person_seq'),
    last_name     varchar not null,
    first_name    varchar not null,
    email         varchar not null,
    date_of_birth date    not null,
    active        boolean not null             default true
);

create sequence event_seq start with 1000;

create table event
(
    id          bigint  not null primary key default nextval('event_seq'),
    title       varchar not null,
    description varchar,
    location    varchar,
    from_date   date    not null,
    to_date     date
);

create sequence registration_seq start with 1000;

create table registration
(
    id         bigint  not null primary key default nextval('registration_seq'),
    year       integer not null,
    open_from  date    not null,
    open_until date    not null
);

create sequence registration_email_seq start with 1000;

create table registration_email
(
    id              bigint  not null primary key default nextval('registration_email_seq'),
    registration_id bigint  not null,
    email           varchar not null,
    link            varchar not null,
    sent_at         timestamp,

    foreign key (registration_id) references registration (id) on delete cascade
);

create table registration_email_person
(
    registration_email_id bigint not null,
    person_id             bigint not null,

    primary key (registration_email_id, person_id),
    foreign key (registration_email_id) references registration_email (id) on delete cascade,
    foreign key (person_id) references person (id)
);

create table registration_person
(
    registration_id bigint not null,
    person_id       bigint not null,

    primary key (registration_id, person_id),
    foreign key (registration_id) references registration (id) on delete cascade,
    foreign key (person_id) references person (id)
);

create table registration_event
(
    registration_id bigint not null,
    event_id        bigint not null,

    primary key (registration_id, event_id),
    foreign key (registration_id) references registration (id) on delete cascade,
    foreign key (event_id) references event (id)
);

create table event_registration
(
    registration_id bigint  not null,
    event_id        bigint  not null,
    person_id       bigint  not null,

    registered      boolean not null default false,

    constraint pk_event_registration primary key (registration_id, event_id, person_id),
    constraint fk_event foreign key (event_id) references event (id),
    constraint fk_person foreign key (person_id) references person (id)
);

create view registration_email_view as
select r.id as registration_id,
       r.year,
       re.email,
       re.link,
       re.sent_at
from registration_email re
         join registration r on re.registration_id = r.id;

create view registration_view as
select r.id,
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