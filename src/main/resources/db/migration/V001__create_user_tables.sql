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
