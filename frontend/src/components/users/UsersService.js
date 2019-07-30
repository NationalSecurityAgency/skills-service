import axios from 'axios';

export default {
  getUserSkillsMetrics(projectId, userId) {
    return axios.get(`/admin/projects/${projectId}/users/${userId}/stats`)
      .then(response => response.data);
  },

  getNumUsersForProject(projectId) {
    return axios.get(`/admin/projects/${projectId}/users/count`)
      .then(response => response.data);
  },

  getUserToken(projectId, userId) {
    return axios.get(`/admin/projects/${projectId}/token/${userId}`)
      .then(response => `${response.data.access_token}`);
  },

  getAvailableVersions(projectId) {
    return axios.get(`/app/projects/${projectId}/versions`)
      .then(response => response.data);
  },

  deleteSkillEvent(projectId, skill) {
    return axios.delete(`/admin/projects/${projectId}/skills/${skill.id}`)
      .then(res => res.data);
  },
};
