import axios from 'axios';

export default {
  getProjects() {
    return axios.get('/app/projects')
      .then(response => response.data);
  },
  getProject(projectId) {
    return axios.get(`/app/projects/${projectId}`).then(response => response.data);
  },
  getProjectDetails(projectId) {
    return axios.get(`/admin/projects/${projectId}`)
      .then(response => response.data);
  },
  changeProjectOrder(projectId, actionToSubmit) {
    return axios.patch(`/admin/projects/${projectId}`, { action: actionToSubmit })
      .then(response => response.data);
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
      .then(response => response.data);
  },
  checkIfProjectIdExist(projectId) {
    if (projectId && projectId.toUpperCase() === 'ALL_SKILLS_PROJECTS') {
      return true;
    }
    return axios.get(`/app/projectExist?projectId=${encodeURIComponent(projectId)}`)
      .then(response => response.data);
  },
  checkIfProjectNameExist(projectName) {
    return axios.get(`/app/projectExist?projectName=${encodeURIComponent(projectName)}`)
      .then(response => response.data);
  },
  checkIfProjectBelongsToGlobalBadge(projectId) {
    return axios.get(`/admin/projects/${projectId}/globalBadge/exists`)
      .then(response => response.data);
  },
};
