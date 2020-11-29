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
  getUserSkillsMetrics(projectId, userId) {
    return axios.get(`/admin/projects/${projectId}/users/${userId}/stats`)
      .then((response) => response.data);
  },

  getUserInfo(projectId, userId) {
    return axios.get(`/admin/projects/${projectId}/users/${userId}`)
      .then((response) => response.data);
  },

  getAvailableVersions(projectId) {
    return axios.get(`/app/projects/${projectId}/versions`)
      .then((response) => response.data);
  },

  deleteSkillEvent(projectId, skill, userId) {
    const timestamp = window.dayjs(skill.performedOn).valueOf();
    return axios.delete(`/admin/projects/${projectId}/skills/${skill.skillId}/users/${userId}/events/${timestamp}`)
      .then((res) => res.data);
  },
};
