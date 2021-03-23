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
delete from PROJECT_DEFINITION where PROJECT_ID <> 'Inception';
delete from USER_ATTRS where USER_ID <> 'root@skills.org' and USER_ID <> 'skills@skills.org';
delete from USER_ROLES where USER_ID = 'skills@skills.org' and (ROLE_NAME = 'ROLE_SUPER_DUPER_USER' OR ROLE_NAME = 'ROLE_SUPERVISOR');
delete from USER_ROLES where USER_ID = 'root@skills.org' and ROLE_NAME = 'ROLE_SUPERVISOR';
delete from GLOBAL_BADGE_LEVEL_DEFINITION;
delete from SKILL_DEFINITION where PROJECT_ID is null;
delete from user_achievement;
delete from SETTINGS where setting_group <> 'project.inception' or setting_group is null;
