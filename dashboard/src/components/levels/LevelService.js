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
  getLevelsForProject(projectId) {
    const url = `/admin/projects/${projectId}/levels`;
    return axios.get(url).then((response) => response.data);
  },
  getLevelsForSubject(projectId, subjectId) {
    const url = `/admin/projects/${projectId}/subjects/${subjectId}/levels`;
    return axios.get(url).then((response) => response.data);
  },
  deleteLastLevelForProject(projectId) {
    const url = `/admin/projects/${projectId}/levels/last`;
    return axios.delete(url, { handleErrorCode: 400 }).then((resp) => resp.data);
  },
  deleteLastLevelForSubject(projectId, subjectId) {
    const url = `/admin/projects/${projectId}/subjects/${subjectId}/levels/last`;
    return axios.delete(url, { handleErrorCode: 400 }).then((resp) => resp.data);
  },
  createNewLevelForProject(projectId, nextLevelObject) {
    const url = `/admin/projects/${projectId}/levels/next`;
    return axios.put(url, nextLevelObject);
  },
  createNewLevelForSubject(projectId, subjectId, nextLevelObject) {
    const url = `/admin/projects/${projectId}/subjects/${subjectId}/levels/next`;
    return axios.put(url, nextLevelObject);
  },
  editLevelForProject(projectId, editedLevel) {
    const url = `/admin/projects/${projectId}/levels/edit/${editedLevel.level}`;
    return axios.put(url, editedLevel);
  },
  editLevelForSubject(projectId, subjectId, editedLevel) {
    const url = `/admin/projects/${projectId}/subjects/${subjectId}/levels/edit/${editedLevel.level}`;
    return axios.put(url, editedLevel);
  },
  getUserLevel(projectId, userId) {
    const url = `/api/projects/${projectId}/level`;
    return axios.get(url, { userId }).then((response) => response.data);
  },
  checkIfProjectLevelBelongsToGlobalBadge(projectId, level) {
    return axios.get(`/admin/projects/${projectId}/levels/${level}/globalBadge/exists`)
      .then((response) => response.data);
  },
};
