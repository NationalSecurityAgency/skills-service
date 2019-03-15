import axios from 'axios';

export default {
  getUserUniqueSkillsCount(projectId, userId) {
    return axios.get(`/admin/projects/${projectId}/users/${userId}/skillsCount`)
      .then(response => response.data);
  },

  getUserToken(projectId, userId) {
    return axios.get(`/admin/projects/${projectId}/token/${userId}`)
      .then(response => `${response.data.access_token}`);
  },
};
