insert into app_authority values('ROLE_USER');
insert into app_authority values('ROLE_ADMIN');


insert into app_user (id, login, password_hash, email, activated) values(1,'admin', '$2a$10$/kRZLg6RKqRmi3sm84v9I.Eekh1fXFXNA98VzqYvYmwV1R/J6hXt6', 'admin@gmail.com',True);
insert into app_user_authority (USER_ID,AUTHORITY_NAME) values(1, 'ROLE_USER');