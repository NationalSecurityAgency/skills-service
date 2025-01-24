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
  getAdminGroupDefs() {
    return axios.get('/app/admin-group-definitions').then((response) => response.data)
  },
  updateAdminGroupDef(adminGroupDef) {
    const opType = adminGroupDef.originalAdminGroupName ? 'admin' : 'app'
    return axios
      .post(`/${opType}/admin-group-definitions/${adminGroupDef.adminGroupId}`, adminGroupDef)
      .then((response) => response.data)
  },
  deleteAdminGroupId(adminGroupId) {
    return axios.delete(`/admin/admin-group-definitions/${adminGroupId}`).then((response) => response.data)
  },
  getAdminGroupDef(adminGroupId) {
    return axios.get(`/admin/admin-group-definitions/${adminGroupId}`).then((response) => response.data)
  },
  getAdminGroupQuizzes(adminGroupId) {
    return axios.get(`/admin/admin-group-definitions/${adminGroupId}/quizzes`).then((response) => response.data)
  },
  getAdminGroupProjects(adminGroupId) {
    return axios.get(`/admin/admin-group-definitions/${adminGroupId}/projects`).then((response) => response.data)
  },
  addQuizToAdminGroup(adminGroupId, quizId) {
    return axios
        .post(`/admin/admin-group-definitions/${adminGroupId}/quizzes/${quizId}`, null,
            { handleError: false })
        .then((response) => response.data)
  },
  removeQuizFromAdminGroup(adminGroupId, quizId) {
    return axios
        .delete(`/admin/admin-group-definitions/${adminGroupId}/quizzes/${quizId}`)
        .then((response) => response.data)
  },
  addProjectToAdminGroup(adminGroupId, projectId) {
    return axios
        .post(`/admin/admin-group-definitions/${adminGroupId}/projects/${projectId}`, null,
            { handleError: false })
        .then((response) => response.data)
  },
  removeProjectFromAdminGroup(adminGroupId, projectId) {
    return axios
        .delete(`/admin/admin-group-definitions/${adminGroupId}/projects/${projectId}`)
        .then((response) => response.data)
  },
  checkIfAdminGroupIdExist(adminGroupId) {
    return axios.post('/app/adminGroupDefExist', { adminGroupId }).then((response) => response.data)
  },
  checkIfAdminGroupNameExist(name) {
    return axios.post('/app/adminGroupDefExist', { name }).then((response) => response.data)
  },
  validateAdminGroupForEnablingCommunity(projectId) {
    return axios
        .get(`/admin/admin-group-definitions/${encodeURIComponent(projectId)}/validateEnablingCommunity`)
        .then((response) => response.data)
  },
  getAdminGroupsForProject(projectId) {
    return axios
        .get(`/admin/projects/${encodeURIComponent(projectId)}/adminGroups`)
        .then((response) => response.data)
  },
  getAdminGroupsForQuiz(quizId) {
    return axios
        .get(`/admin/quiz-definitions/${encodeURIComponent(quizId)}/adminGroups`)
        .then((response) => response.data)
  },
}
