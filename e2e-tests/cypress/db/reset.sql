-- creating Inception project is expensive so lets not delete it
delete from PROJECT_DEFINITION where project_id <> 'Inception';
delete from USER_ATTRS where user_id <> 'root@skills.org';
