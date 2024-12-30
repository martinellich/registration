create sequence security_group_seq start with 1000;

create table security_group
(
    id   bigint  not null default nextval('security_group_seq') primary key,

    name varchar not null
);

create sequence security_user_seq start with 1000;

create table security_user
(
    id         bigint  not null default nextval('security_user_seq') primary key,

    first_name varchar not null,
    last_name  varchar not null,
    email      varchar not null,
    secret     varchar not null
);

create table user_group
(
    user_id  bigint not null,
    group_id bigint not null,

    foreign key (user_id) references security_user (id),
    foreign key (group_id) references security_group (id)
);

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

    foreign key (registration_id) references registration (id)
);

create table registration_email_person
(
    registration_email_id bigint not null,
    person_id             bigint not null,

    primary key (registration_email_id, person_id),
    foreign key (registration_email_id) references registration_email (id),
    foreign key (person_id) references person (id)
);

create table registration_person
(
    registration_id bigint not null,
    person_id       bigint not null,

    primary key (registration_id, person_id),
    foreign key (registration_id) references registration (id),
    foreign key (person_id) references person (id)
);

create table registration_event
(
    registration_id bigint not null,
    event_id        bigint not null,

    primary key (registration_id, event_id),
    foreign key (registration_id) references registration (id),
    foreign key (event_id) references event (id)
);

create table event_registration
(
    event_id   bigint  not null,
    person_id  bigint  not null,

    registered boolean not null default false,

    constraint pk_event_registration primary key (event_id, person_id),
    constraint fk_event foreign key (event_id) references event (id),
    constraint fk_person foreign key (person_id) references person (id)
);

create view event_registration_view as
select e.id as event_id,
       e.title,
       e.description,
       e.location,
       e.from_date,
       e.to_date,
       p.id as person_id,
       p.last_name,
       p.first_name,
       p.email,
       p.date_of_birth,
       r.registered
from event e
         join event_registration r on e.id = r.event_id
         join person p on r.person_id = p.id;

create view registration_email_view as
select r.year,
       re.email,
       re.link,
       re.sent_at
from registration_email re
         join registration r on re.registration_id = r.id