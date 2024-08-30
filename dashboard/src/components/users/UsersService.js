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

  ajaxDownload(url, params) {
    return axios.get(url, { params, responseType: 'blob' }).then((response) => {
      // response.data
      // create file link in browser's memory
      const href = URL.createObjectURL(response.data);

      // create "a" HTML element with href to file & click
      const link = document.createElement('a');
      link.href = href;
      let filename = "";
      const disposition = response.headers['content-disposition']
      if (disposition && disposition.indexOf('attachment') !== -1) {
        const filenameRegex = /fileName[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
        const matches = filenameRegex.exec(disposition);
        if (matches != null && matches[1]) {
          filename = matches[1].replace(/['"]/g, '');
        }
      }
      link.setAttribute('download', filename); //or any other extension
      link.click();

      // remove ObjectURL
      URL.revokeObjectURL(href);
    })
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
  }
}
