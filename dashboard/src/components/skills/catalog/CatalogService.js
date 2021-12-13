/*
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import axios from 'axios';

export default {
  getCatalogSkills(projectId, params) {
    const url = `/admin/projects/${projectId}/skills/catalog`;
    return axios.get(url, { params })
      .then((response) => response.data);
  },
  bulkExport(projectId, skillIds) {
    const url = `/admin/projects/${projectId}/skills/export`;
    return axios.post(url, skillIds)
      .then((response) => response.data);
  },
  import(projectId, subjectId, fromProjectId, fromSkillId) {
    const url = `/admin/projects/${projectId}/subjects/${subjectId}/import/${fromProjectId}/${fromSkillId}`;
    return axios.post(url)
      .then((response) => response.data);
  },
  bulkImport(projectId, subjectId, listOfProjectAndSkillIds) {
    const url = `/admin/projects/${projectId}/subjects/${subjectId}/import`;
    return axios.post(url, listOfProjectAndSkillIds)
      .then((response) => response.data);
  },
  getExportedStats(projectId, skillId) {
    const url = `/admin/projects/${projectId}/skills/${skillId}/exported/stats`;
    return axios.get(url)
      .then((response) => response.data);
  },
  removeExportedSkill(projectId, skillId) {
    return axios.delete(`/admin/projects/${projectId}/skills/${skillId}/export`).then((response) => response.data);
  },
};
