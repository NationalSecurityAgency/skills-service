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
  getProjects() {
    const url = '/app/projects'
    return axios.get(url).then((response) => response.data)
  },
  getAvailableForMyProjects() {
    const url = '/api/availableForMyProjects'
    return axios.get(url).then((response) => response.data)
  },
  addToMyProjects(projectId) {
    const url = `/api/myprojects/${encodeURIComponent(projectId)}`
    return axios.post(url).then((response) => response.data)
  },
  moveMyProject(projectId, newSortIndex) {
    const url = `/api/myprojects/${encodeURIComponent(projectId)}`
    return axios.post(url, { newSortIndex }).then((response) => response.data)
  },
  removeFromMyProjects(projectId) {
    const url = `/api/myprojects/${encodeURIComponent(projectId)}`
    return axios.delete(url).then((response) => response.data)
  },
  searchProjects(search) {
    const url = `/root/searchProjects?name=${search}`
    return axios.get(url).then((response) => response.data)
  },
  loadAllProjects() {
    return axios.get('/root/projects').then((response) => response.data)
  },
  getProject(projectId) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}`)
      .then((response) => response.data)
  },
  getProjectErrors(projectId, params) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/errors`, { params })
      .then((response) => response.data)
  },
  deleteProjectError(projectId, errorId) {
    return axios
      .delete(`/admin/projects/${encodeURIComponent(projectId)}/errors/${errorId}`)
      .then((response) => response.data)
  },
  deleteAllProjectErrors(projectId) {
    return axios
      .delete(`/admin/projects/${encodeURIComponent(projectId)}/errors`)
      .then((response) => response.data)
  },
  getProjectDetails(projectId) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}`)
      .then((response) => response.data)
  },
  countProjectUsers(projectId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/users/count`).then((response) => response.data)
  },
  getLatestSkillEventForProject(projectId) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/lastSkillEvent`)
      .then((response) => response.data)
  },
  updateProjectDisplaySortOrder(projectId, newDisplayOrderIndex) {
    return axios.patch(`/admin/projects/${encodeURIComponent(projectId)}`, {
      action: 'NewDisplayOrderIndex',
      newDisplayOrderIndex
    })
  },
  saveProject(project) {
    if (project.isEdit) {
      return axios
        .post(`/admin/projects/${encodeURIComponent(project.originalProjectId)}`, project)
        .then(() => this.getProject(project.projectId))
    }
    return axios
      .post(`/app/projects/${encodeURIComponent(project.projectId)}`, project)
      .then(() => this.getProject(project.projectId))
  },
  copyProject(oringinalProjectId, newProject) {
    return axios
      .post(`/admin/projects/${encodeURIComponent(oringinalProjectId)}/copy`, newProject)
      .then((res) => res.data)
  },
  deleteProject(projectId) {
    return axios.delete(`/admin/projects/${encodeURIComponent(projectId)}`)
  },
  queryOtherProjectsByName(projectId, nameQuery) {
    return axios
      .get(
        `/admin/projects/${encodeURIComponent(projectId)}/projectSearch?nameQuery=${encodeURIComponent(nameQuery)}`
      )
      .then((response) => response.data)
  },
  checkIfProjectIdExist(projectId) {
    if (projectId && projectId.toUpperCase() === 'ALL_SKILLS_PROJECTS') {
      return true
    }
    return axios.post('/app/projectExist', { projectId }).then((response) => response.data)
  },
  checkIfProjectNameExist(projectName) {
    return axios.post('/app/projectExist', { name: projectName }).then((response) => response.data)
  },
  checkIfProjectBelongsToGlobalBadge(projectId) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/globalBadge/exists`)
      .then((response) => response.data)
  },
  cancelUnusedProjectDeletion(projectId) {
    return axios
      .post(`/admin/projects/${encodeURIComponent(projectId)}/cancelExpiration`)
      .then((response) => response.data)
  },
  countUsersMatchingCriteria(projectId, userQueryCriteria) {
    return axios
      .post(`/admin/projects/${encodeURIComponent(projectId)}/contactUsersCount`, userQueryCriteria)
      .then((response) => response.data)
  },
  contactUsers(projectId, contactUsersRequest) {
    return axios
      .post(`/admin/projects/${encodeURIComponent(projectId)}/contactUsers`, contactUsersRequest)
      .then((response) => response.data)
  },
  countProjectAdmins() {
    return axios.get('/root/users/countAllProjectAdmins').then((response) => response.data)
  },
  contactProjectAdmins(email) {
    return axios
      .post('/root/users/contactAllProjectAdmins', email)
      .then((response) => response.data)
  },
  previewEmail(projectId, email) {
    return axios
      .post(`/admin/projects/${encodeURIComponent(projectId)}/previewEmail`, email)
      .then((response) => response.data)
  },
  rootPreviewEmail(email) {
    return axios.post('/root/users/previewEmail', email).then((response) => response.data)
  },
  loadDescription(projectId) {
    return axios
      .get(`/app/projects/${encodeURIComponent(projectId)}/description`)
      .then((response) => response.data)
  },
  validateProjectForEnablingCommunity(projectId) {
    return axios
      .get(`/admin/projects/${encodeURIComponent(projectId)}/validateEnablingCommunity`)
      .then((response) => response.data)
  }
}
