-- Copyright 2020 SkillTree
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     https://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- creating Inception project is expensive so lets not delete it
delete from project_definition where project_id <> 'Inception';
delete from quiz_definition;
delete from admin_group_definition;
delete from user_attrs where user_id <> 'root@skills.org' and user_id <> 'skills@skills.org' and user_id <> 'ai-grader';
delete from user_roles where user_id = 'skills@skills.org' and (role_name = 'ROLE_SUPER_DUPER_USER' OR role_name = 'ROLE_GLOBAL_BADGE_ADMIN' OR role_name = 'ROLE_PROJECT_ADMIN' OR role_name = 'ROLE_PRIVATE_PROJECT_USER' OR role_name = 'ROLE_DASHBOARD_ADMIN_ACCESS');
delete from user_roles where user_id = 'root@skills.org' and role_name = 'ROLE_GLOBAL_BADGE_ADMIN';
delete from global_badge_level_definition;
delete from skill_definition where project_id is null;
delete from user_achievement;
delete from settings where setting_group <> 'project.inception' or setting_group is null;
delete from user_points where project_id = 'Inception';
delete from user_achievement where project_id = 'Inception';
delete from user_performed_skill where project_id = 'Inception';
delete from user_events where project_id = 'Inception';
delete from user_tags;
delete from notifications;
delete from user_actions_history;
delete from custom_icons;
delete from web_notifications;
delete from web_notifications_ack;
delete from attachments;