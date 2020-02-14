-- creating Inception project is expensive so lets not delete it
delete from PROJECT_DEFINITION where PROJECT_ID <> 'Inception';
delete from USER_ATTRS where USER_ID <> 'root@skills.org' and USER_ID <> 'skills@skills.org';
delete from USER_ROLES where USER_ID = 'skills@skills.org' and ROLE_NAME = 'ROLE_SUPER_DUPER_USER';
delete from GLOBAL_BADGE_LEVEL_DEFINITION;
delete from SKILL_DEFINITION where PROJECT_ID is null;
