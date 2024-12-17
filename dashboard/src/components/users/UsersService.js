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
import dayjs from '@/common-components/DayJsCustomizer'

export default {
  ajaxCall(url, params) {
    return axios.get(url, { params }).then((response) => response.data)
  },

  getUserSkillsMetrics(projectId, userId) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/users/${encodeURIComponent(userId)}/stats`
      )
      .then((response) => response.data)
  },

  getUserInfo(projectId, userId) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/users/${encodeURIComponent(userId)}`)
      .then((response) => response.data)
  },

  getAvailableVersions(projectId) {
    return axios
      .get(`/app/projects/${encodeURIComponent(projectId)}/versions`)
      .then((response) => response.data)
  },

  bulkDeleteSkillEvents(projectId, userId, skills) {
    return axios.delete(
        `/admin/projects/${encodeURIComponent(projectId)}/users/${encodeURIComponent(userId)}/events/bulkDelete`, { data: skills }
    ).then((res) => res.data)
  },
  deleteSkillEvent(projectId, skill, userId) {
    const timestamp = dayjs(skill.performedOn).valueOf()
    return axios
      .delete(
        `/admin/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skill.skillId)}/users/${encodeURIComponent(userId)}/events/${timestamp}`
      )
      .then((res) => res.data)
  },
  deleteAllSkillEvents(projectId, userId) {
    return axios
      .delete(
        `/admin/projects/${encodeURIComponent(projectId)}/users/${encodeURIComponent(userId)}/events`
      )
      .then((res) => res.data)
  },
  canAccess(projectId, userId) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/${encodeURIComponent(userId)}/canAccess`
      )
      .then((response) => response.data)
  },
  getUserTags(userId) {
    return axios.get(`/app/userInfo/userTags/${userId}`).then((response) => response.data)
  },
  archiveUsers(projectId, userIds) {
    return axios.post(`/admin/projects/${encodeURIComponent(projectId)}/users/archive`, { userIds})
        .then((res) => res.data)
  },
  getArchivedUsers(projectId, params) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/users/archive`, { params } ).then((response) => response.data)
  },
  restoreArchivedUser(projectId, userId) {
    return axios.post(`/admin/projects/${encodeURIComponent(projectId)}/users/${userId}/restore`)
        .then((res) => res.data)
  },
  isUserArchived(projectId, userId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/users/${userId}/isArchived`)
        .then((res) => res.data.success)
  }
}
