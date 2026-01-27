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
delete from project_definition;
delete from quiz_definition;
delete from admin_group_definition;
delete from user_attrs;
delete from user_roles;
delete from global_badge_level_definition;
delete from skill_definition;
delete from settings;
delete from user_tags;
delete from user_actions_history;
delete from custom_icons;
delete from web_notifications;
delete from web_notifications_ack;
delete from attachments;
delete from vector_store;
