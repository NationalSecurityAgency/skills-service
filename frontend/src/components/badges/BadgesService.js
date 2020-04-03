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
  getBadges(projectId) {
    return axios.get(`/admin/projects/${projectId}/badges`)
      .then(response => response.data);
  },
  getBadge(projectId, badgeId) {
    return axios.get(`/admin/projects/${projectId}/badges/${badgeId}`)
      .then(response => response.data);
  },
  saveBadge(badgeReq) {
    if (badgeReq.isEdit) {
      return axios.post(`/admin/projects/${badgeReq.projectId}/badges/${badgeReq.originalBadgeId}`, badgeReq)
        .then(() => this.getBadge(badgeReq.projectId, badgeReq.badgeId));
    }

    const req = Object.assign({ enabled: false }, badgeReq);
    return axios.post(`/admin/projects/${req.projectId}/badges/${req.badgeId}`, req)
      .then(() => this.getBadge(req.projectId, req.badgeId));
  },
  deleteBadge(projectId, badgeId) {
    return axios.delete(`/admin/projects/${projectId}/badges/${badgeId}`)
      .then(repsonse => repsonse.data);
  },
  moveBadge(projectId, badgeId, actionToSubmit) {
    return axios.patch(`/admin/projects/${projectId}/badges/${badgeId}`, {
      action: actionToSubmit,
    })
      .then(response => response.data);
  },
  badgeWithNameExists(projectId, badgeName) {
    return axios.post(`/admin/projects/${projectId}/badgeNameExists`, { name: badgeName })
      .then(remoteRes => !remoteRes.data);
  },
  badgeWithIdExists(projectId, badgeId) {
    return axios.get(`/admin/projects/${projectId}/entityIdExists?id=${badgeId}`)
      .then(remoteRes => !remoteRes.data);
  },
};
