import axios from 'axios';

export default {
  getHostInfo(projectId) {
    return axios.get(`/admin/projects/${projectId}/hostInfo`)
      .then(response => response.data);
  },
};
