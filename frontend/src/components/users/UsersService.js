import axios from 'axios';

export default {
  getUserSkillsMetrics(projectId, userId) {
    return axios.get(`/admin/projects/${projectId}/users/${userId}/stats`)
      .then(response => response.data);
  },

  getUserInfo(projectId, userId) {
    return axios.get(`/admin/projects/${projectId}/users/${userId}`)
      .then(response => response.data);
  },

  getAvailableVersions(projectId) {
    return axios.get(`/app/projects/${projectId}/versions`)
      .then(response => response.data);
  },

  deleteSkillEvent(projectId, skill, userId) {
    const timestamp = window.moment(skill.performedOn).valueOf();
    return axios.delete(`/admin/projects/${projectId}/skills/${skill.skillId}/users/${userId}/events/${timestamp}`)
      .then(res => res.data);
  },
};
