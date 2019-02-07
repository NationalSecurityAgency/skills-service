import axios from 'axios';

export default {
  getProjects() {
    return axios.get('/app/projects')
      .then(response => response.data);
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
    return axios.put(`/app/projects/${project.projectId}`, project);
  },
  deleteProject(projectId) {
    return axios.delete(`/admin/projects/${projectId}`);
  },
  queryOtherProjectsByName(projectId, nameQuery) {
    return axios.get(`/admin/projects/${projectId}/projectSearch?nameQuery=${nameQuery}`)
      .then(response => response.data);
  },
};
