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
  getProjects() {
    const url = '/app/projects';
    return axios.get(url)
      .then((response) => response.data);
  },
  searchProjects(search) {
    const url = `/root/searchProjects?name=${search}`;
    return axios.get(url)
      .then((response) => response.data);
  },
  loadAllProjects() {
    return axios.get('/root/projects')
      .then((response) => response.data);
  },
  getProject(projectId) {
    return axios.get(`/admin/projects/${projectId}`).then((response) => response.data);
  },
  getProjectErrors(projectId) {
    return axios.get(`/admin/projects/${projectId}/errors`).then((response) => response.data);
  },
  deleteProjectError(projectId, reportedSkillid) {
    return axios.delete(`/admin/projects/${projectId}/errors/${encodeURIComponent(reportedSkillid)}`).then((response) => response.data);
  },
  deleteAllProjectErrors(projectId) {
    return axios.delete(`/admin/projects/${projectId}/errors`).then((response) => response.data);
  },
  getProjectDetails(projectId) {
    return axios.get(`/admin/projects/${projectId}`)
      .then((response) => response.data);
  },
  changeProjectOrder(projectId, actionToSubmit) {
    return axios.patch(`/admin/projects/${projectId}`, { action: actionToSubmit })
      .then((response) => response.data);
  },
  saveProject(project) {
    if (project.isEdit) {
      return axios.post(`/admin/projects/${project.originalProjectId}`, project)
        .then(() => this.getProject(project.projectId));
    }
    return axios.post(`/app/projects/${project.projectId}`, project)
      .then(() => this.getProject(project.projectId));
  },
  deleteProject(projectId) {
    return axios.delete(`/admin/projects/${projectId}`);
  },
  queryOtherProjectsByName(projectId, nameQuery) {
    return axios.get(`/admin/projects/${projectId}/projectSearch?nameQuery=${encodeURIComponent(nameQuery)}`)
      .then((response) => response.data);
  },
  checkIfProjectIdExist(projectId) {
    if (projectId && projectId.toUpperCase() === 'ALL_SKILLS_PROJECTS') {
      return true;
    }
    return axios.post('/app/projectExist', { projectId })
      .then((response) => response.data);
  },
  checkIfProjectNameExist(projectName) {
    return axios.post('/app/projectExist', { name: projectName })
      .then((response) => response.data);
  },
  checkIfProjectBelongsToGlobalBadge(projectId) {
    return axios.get(`/admin/projects/${projectId}/globalBadge/exists`)
      .then((response) => response.data);
  },
};
