--
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
--


INSERT INTO public.project_definition (id, project_id, name, client_secret, total_points, created, updated) VALUES (6, 'SampleProject', 'Sample Project', '9X2FFx500Et9AqArPtg86kGbE6XH82u5', 150, '2022-05-17 14:12:18.311101', '2022-05-17 14:12:47.824000');
INSERT INTO public.skill_definition (id, project_id, skill_id, proj_ref_id, name, point_increment, point_increment_interval, increment_interval_max_occurrences, total_points, description, help_url, display_order, type, custom_icon_ref_id, icon_class, start_date, end_date, version, created, updated, enabled, self_reporting_type, num_skills_required, group_id, read_only, copied_from_skill_ref, copied_from_project_id) VALUES (65, 'SampleProject', 'Skill3Skill', null, 'Skill3', 10, 480, 1, 50, null, null, 3, 'Skill', null, null, null, null, 0, '2022-05-17 14:12:47.805000', '2022-05-17 14:12:47.805000', 'true', 'Approval', -1, null, 'null', null, null);
INSERT INTO public.skill_definition (id, project_id, skill_id, proj_ref_id, name, point_increment, point_increment_interval, increment_interval_max_occurrences, total_points, description, help_url, display_order, type, custom_icon_ref_id, icon_class, start_date, end_date, version, created, updated, enabled, self_reporting_type, num_skills_required, group_id, read_only, copied_from_skill_ref, copied_from_project_id) VALUES (62, 'SampleProject', 'Subject1Subject', 6, 'Subject 1', 0, 0, 0, 150, '17052', null, 1, 'Subject', null, 'fas fa-book', null, null, 0, '2022-05-17 14:12:24.197000', '2022-05-17 14:12:47.817000', 'true', null, -1, null, 'false', null, null);
INSERT INTO public.skill_definition (id, project_id, skill_id, proj_ref_id, name, point_increment, point_increment_interval, increment_interval_max_occurrences, total_points, description, help_url, display_order, type, custom_icon_ref_id, icon_class, start_date, end_date, version, created, updated, enabled, self_reporting_type, num_skills_required, group_id, read_only, copied_from_skill_ref, copied_from_project_id) VALUES (63, 'SampleProject', 'Skill1Skill', null, 'Skill1', 10, 480, 1, 50, null, null, 1, 'Skill', null, null, null, null, 0, '2022-05-17 14:12:32.623000', '2022-05-17 14:12:32.623000', 'true', null, -1, null, 'null', null, null);
INSERT INTO public.skill_definition (id, project_id, skill_id, proj_ref_id, name, point_increment, point_increment_interval, increment_interval_max_occurrences, total_points, description, help_url, display_order, type, custom_icon_ref_id, icon_class, start_date, end_date, version, created, updated, enabled, self_reporting_type, num_skills_required, group_id, read_only, copied_from_skill_ref, copied_from_project_id) VALUES (64, 'SampleProject', 'Skill2Skill', null, 'Skill2', 10, 480, 1, 50, null, null, 2, 'Skill', null, null, null, null, 0, '2022-05-17 14:12:40.825000', '2022-05-17 14:12:40.825000', 'true', 'HonorSystem', -1, null, 'null', null, null);
INSERT INTO public.skill_definition (id, project_id, skill_id, proj_ref_id, name, point_increment, point_increment_interval, increment_interval_max_occurrences, total_points, description, help_url, display_order, type, custom_icon_ref_id, icon_class, start_date, end_date, version, created, updated, enabled, self_reporting_type, num_skills_required, group_id, read_only, copied_from_skill_ref, copied_from_project_id) VALUES (66, 'SampleProject', 'SampleBadgeBadge', 6, 'Sample Badge', 0, 0, 0, 0, '17053', null, 1, 'Badge', null, 'fas fa-award', null, null, 0, '2022-05-17 14:26:35.618000', '2022-05-17 14:26:35.618000', 'false', null, -1, null, 'null', null, null);
INSERT INTO public.skill_relationship_definition (id, parent_ref_id, child_ref_id, type, created, updated) VALUES (57, 62, 63, 'RuleSetDefinition', '2022-05-17 14:12:32.635000', '2022-05-17 14:12:32.635000');
INSERT INTO public.skill_relationship_definition (id, parent_ref_id, child_ref_id, type, created, updated) VALUES (58, 62, 64, 'RuleSetDefinition', '2022-05-17 14:12:40.831000', '2022-05-17 14:12:40.831000');
INSERT INTO public.skill_relationship_definition (id, parent_ref_id, child_ref_id, type, created, updated) VALUES (59, 62, 65, 'RuleSetDefinition', '2022-05-17 14:12:47.809000', '2022-05-17 14:12:47.809000');
INSERT INTO public.level_definition (id, project_ref_id, skill_ref_id, level, percent, points_from, points_to, custom_icon_ref_id, icon_class, logical_name, created, updated) VALUES (51, 6, null, 1, 10, null, null, null, 'fas fa-user-ninja', 'White Belt', '2022-05-17 14:12:18.311101', '2022-05-17 14:12:18.311101');
INSERT INTO public.level_definition (id, project_ref_id, skill_ref_id, level, percent, points_from, points_to, custom_icon_ref_id, icon_class, logical_name, created, updated) VALUES (52, 6, null, 2, 25, null, null, null, 'fas fa-user-ninja', 'Blue Belt', '2022-05-17 14:12:18.311101', '2022-05-17 14:12:18.311101');
INSERT INTO public.level_definition (id, project_ref_id, skill_ref_id, level, percent, points_from, points_to, custom_icon_ref_id, icon_class, logical_name, created, updated) VALUES (53, 6, null, 3, 45, null, null, null, 'fas fa-user-ninja', 'Purple Belt', '2022-05-17 14:12:18.311101', '2022-05-17 14:12:18.311101');
INSERT INTO public.level_definition (id, project_ref_id, skill_ref_id, level, percent, points_from, points_to, custom_icon_ref_id, icon_class, logical_name, created, updated) VALUES (54, 6, null, 4, 67, null, null, null, 'fas fa-user-ninja', 'Brown Belt', '2022-05-17 14:12:18.311101', '2022-05-17 14:12:18.311101');
INSERT INTO public.level_definition (id, project_ref_id, skill_ref_id, level, percent, points_from, points_to, custom_icon_ref_id, icon_class, logical_name, created, updated) VALUES (55, 6, null, 5, 92, null, null, null, 'fas fa-user-ninja', 'Black Belt', '2022-05-17 14:12:18.311101', '2022-05-17 14:12:18.311101');
INSERT INTO public.level_definition (id, project_ref_id, skill_ref_id, level, percent, points_from, points_to, custom_icon_ref_id, icon_class, logical_name, created, updated) VALUES (56, null, 62, 1, 10, null, null, null, 'fas fa-user-ninja', 'White Belt', '2022-05-17 14:12:24.146642', '2022-05-17 14:12:24.146642');
INSERT INTO public.level_definition (id, project_ref_id, skill_ref_id, level, percent, points_from, points_to, custom_icon_ref_id, icon_class, logical_name, created, updated) VALUES (57, null, 62, 2, 25, null, null, null, 'fas fa-user-ninja', 'Blue Belt', '2022-05-17 14:12:24.146642', '2022-05-17 14:12:24.146642');
INSERT INTO public.level_definition (id, project_ref_id, skill_ref_id, level, percent, points_from, points_to, custom_icon_ref_id, icon_class, logical_name, created, updated) VALUES (58, null, 62, 3, 45, null, null, null, 'fas fa-user-ninja', 'Purple Belt', '2022-05-17 14:12:24.146642', '2022-05-17 14:12:24.146642');
INSERT INTO public.level_definition (id, project_ref_id, skill_ref_id, level, percent, points_from, points_to, custom_icon_ref_id, icon_class, logical_name, created, updated) VALUES (59, null, 62, 4, 67, null, null, null, 'fas fa-user-ninja', 'Brown Belt', '2022-05-17 14:12:24.146642', '2022-05-17 14:12:24.146642');
INSERT INTO public.level_definition (id, project_ref_id, skill_ref_id, level, percent, points_from, points_to, custom_icon_ref_id, icon_class, logical_name, created, updated) VALUES (60, null, 62, 5, 92, null, null, null, 'fas fa-user-ninja', 'Black Belt', '2022-05-17 14:12:24.146642', '2022-05-17 14:12:24.146642');





