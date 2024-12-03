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
drop table IF EXISTS databasechangeloglock CASCADE;

drop table IF EXISTS databasechangelog CASCADE;

drop table IF EXISTS skills_db_locks CASCADE;

drop table IF EXISTS user_actions_history CASCADE;

drop table IF EXISTS skill_relationship_definition CASCADE;

drop table IF EXISTS skill_share_definition CASCADE;

drop table IF EXISTS user_roles CASCADE;

drop table IF EXISTS user_performed_skill CASCADE;

drop table IF EXISTS user_points CASCADE;

drop table IF EXISTS user_achievement CASCADE;

drop table IF EXISTS settings CASCADE;

drop table IF EXISTS global_badge_level_definition CASCADE;

drop table IF EXISTS level_definition CASCADE;

drop table IF EXISTS user_events CASCADE;

drop table IF EXISTS skill_approval CASCADE;

drop table IF EXISTS exported_skills CASCADE;

drop table IF EXISTS skill_updated_queue CASCADE;

drop table IF EXISTS skill_attributes_definition CASCADE;

drop table IF EXISTS skill_definition CASCADE;

drop table IF EXISTS custom_icons CASCADE;

drop table IF EXISTS project_error CASCADE;

drop table IF EXISTS project_access_token CASCADE;

drop table IF EXISTS project_definition CASCADE;

drop table IF EXISTS user_token CASCADE;

drop table IF EXISTS notifications CASCADE;

drop table IF EXISTS users CASCADE;

drop table IF EXISTS user_tags CASCADE;

drop table IF EXISTS user_attrs CASCADE;

drop table IF EXISTS scheduled_tasks CASCADE;

drop table IF EXISTS client_preferences CASCADE;

drop table IF EXISTS attachments CASCADE;

drop table IF EXISTS skill_approval_conf CASCADE;

drop table IF EXISTS user_quiz_answer_graded CASCADE;

drop table IF EXISTS quiz_answer_definition CASCADE;

drop table IF EXISTS quiz_definition CASCADE;

drop table IF EXISTS quiz_question_definition CASCADE;

drop table IF EXISTS quiz_settings CASCADE;

drop table IF EXISTS quiz_to_skill_definition CASCADE;

drop table IF EXISTS user_quiz_answer_attempt CASCADE;

drop table IF EXISTS user_quiz_attempt CASCADE;

drop table IF EXISTS user_quiz_question_attempt CASCADE;

drop table IF EXISTS expired_user_achievement CASCADE;

drop table IF EXISTS custom_icons CASCADE;

drop table IF EXISTS admin_group_definition CASCADE;

drop table IF EXISTS user_quiz_answer_graded CASCADE;

drop table IF EXISTS archived_users CASCADE;

drop function IF EXISTS f_select_lock_and_insert;
