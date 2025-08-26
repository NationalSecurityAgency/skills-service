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
import axios from 'axios'

export default {
  getBadges() {
    return axios.get('/app/badges').then((response) => response.data)
  },
  getBadge(badgeId) {
    return axios
      .get(`/admin/badges/${encodeURIComponent(badgeId)}`)
      .then((response) => response.data)
  },
  saveBadge(badgeReq) {
    if (badgeReq.isEdit) {
      return axios
        .put(`/admin/badges/${encodeURIComponent(badgeReq.originalBadgeId)}`, badgeReq)
        .then(() => this.getBadge(badgeReq.badgeId))
    }
    const req = { enabled: false, ...badgeReq }
    return axios
      .put(`/app/badges/${encodeURIComponent(req.badgeId)}`, req)
      .then(() => this.getBadge(req.badgeId))
  },
  deleteBadge(badgeId) {
    return axios
      .delete(`/admin/badges/${encodeURIComponent(badgeId)}`)
      .then((response) => response.data)
  },
  updateBadgeDisplaySortOrder(badgeId, newDisplayOrderIndex) {
    return axios.patch(`/admin/badges/${encodeURIComponent(badgeId)}`, {
      action: 'NewDisplayOrderIndex',
      newDisplayOrderIndex
    })
  },
  badgeWithNameExists(badgeName) {
    return axios
      .post('/app/badges/name/exists', { name: badgeName })
      .then((remoteRes) => !remoteRes.data)
  },
  badgeWithIdExists(badgeId) {
    return axios
      .get(`/app/badges/id/${encodeURIComponent(badgeId)}/exists`)
      .then((remoteRes) => !remoteRes.data)
  },
  assignSkillToBadge(badgeId, projectId, skillId) {
    return axios
      .post(
        `/admin/badges/${encodeURIComponent(badgeId)}/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}`
      )
      .then((res) => res.data)
  },
  removeSkillFromBadge(badgeId, projectId, skillId) {
    return axios
      .delete(
        `/admin/badges/${encodeURIComponent(badgeId)}/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}`
      )
      .then((res) => res.data)
  },
  getBadgeSkills(badgeId) {
    return axios
      .get(`/admin/badges/${encodeURIComponent(badgeId)}/skills`)
      .then((res) => res.data)
  },
  suggestProjectSkills(badgeId, search) {
    return axios
      .get(`/admin/badges/${encodeURIComponent(badgeId)}/skills/available?query=${search}`)
      .then((res) => res.data)
  },
  suggestProjectsForPage(badgeId, search) {
    return axios
      .get(`/admin/badges/${encodeURIComponent(badgeId)}/projects/available?query=${search}`)
      .then((res) => res.data)
  },
  getProjectLevels(projectId) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/levels`)
      .then((res) => res.data)
  },
  assignProjectLevelToBadge(badgeId, projectId, levelId) {
    return axios
      .post(
        `/admin/badges/${encodeURIComponent(badgeId)}/projects/${encodeURIComponent(projectId)}/level/${encodeURIComponent(levelId)}`
      )
      .then((res) => res.data)
  },
  removeProjectLevelFromBadge(badgeId, projectId, level) {
    return axios
      .delete(
        `/admin/badges/${encodeURIComponent(badgeId)}/projects/${encodeURIComponent(projectId)}/level/${level}`
      )
      .then((res) => res.data)
  },
  changeProjectLevel(badgeId, projectId, oldLevel, newLevel) {
    return axios
      .post(
        `/admin/badges/${encodeURIComponent(badgeId)}/projects/${encodeURIComponent(projectId)}/level/${oldLevel}/${newLevel}`
      )
      .then((res) => res.data)
  },
  validateAdminGroupForEnablingCommunity(badgeId) {
    return axios
      .get(`/admin/badges/${encodeURIComponent(badgeId)}/validateEnablingCommunity`)
      .then((response) => response.data)
  },
}
