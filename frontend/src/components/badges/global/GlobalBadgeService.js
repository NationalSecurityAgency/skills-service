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
  getBadges() {
    return axios.get('/supervisor/badges')
      .then(response => response.data);
  },
  getBadge(badgeId) {
    return axios.get(`/supervisor/badges/${badgeId}`)
      .then(response => response.data);
  },
  saveBadge(badgeReq) {
    if (badgeReq.isEdit) {
      return axios.put(`/supervisor/badges/${badgeReq.originalBadgeId}`, badgeReq)
        .then(() => this.getBadge(badgeReq.badgeId));
    }
    return axios.put(`/supervisor/badges/${badgeReq.badgeId}`, badgeReq)
      .then(() => this.getBadge(badgeReq.badgeId));
  },
  deleteBadge(badgeId) {
    return axios.delete(`/supervisor/badges/${badgeId}`)
      .then(response => response.data);
  },
  moveBadge(badgeId, actionToSubmit) {
    return axios.patch(`/supervisor/badges/${badgeId}`, {
      action: actionToSubmit,
    })
      .then(response => response.data);
  },
  badgeWithNameExists(badgeName) {
    return axios.get(`/supervisor/badges/name/${encodeURIComponent(badgeName)}/exists`)
      .then(remoteRes => !remoteRes.data);
  },
  badgeWithIdExists(badgeId) {
    return axios.get(`/supervisor/badges/id/${encodeURIComponent(badgeId)}/exists`)
      .then(remoteRes => !remoteRes.data);
  },
  assignSkillToBadge(badgeId, projectId, skillId) {
    return axios.post(`/supervisor/badges/${badgeId}/projects/${projectId}/skills/${skillId}`)
      .then(res => res.data);
  },
  removeSkillFromBadge(badgeId, projectId, skillId) {
    return axios.delete(`/supervisor/badges/${badgeId}/projects/${projectId}/skills/${skillId}`)
      .then(res => res.data);
  },
  getBadgeSkills(badgeId) {
    return axios.get(`/supervisor/badges/${badgeId}/skills`)
      .then(res => res.data);
  },
  suggestProjectSkills(badgeId, search) {
    return axios.get(`/supervisor/badges/${badgeId}/skills/available?query=${search}`)
      .then(res => res.data);
  },
  getAllProjectsForBadge(badgeId) {
    return axios.get(`/supervisor/badges/${badgeId}/projects/available`)
      .then(res => res.data);
  },
  getProjectLevels(projectId) {
    return axios.get(`/supervisor/projects/${projectId}/levels`)
      .then(res => res.data);
  },
  assignProjectLevelToBadge(badgeId, projectId, levelId) {
    return axios.post(`/supervisor/badges/${badgeId}/projects/${projectId}/level/${levelId}`)
      .then(res => res.data);
  },
  removeProjectLevelFromBadge(badgeId, projectId, level) {
    return axios.delete(`/supervisor/badges/${badgeId}/projects/${projectId}/level/${level}`)
      .then(res => res.data);
  },
};
