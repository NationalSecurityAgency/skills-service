import axios from 'axios';

export default {
  getCustomIconCss(projectId) {
    return axios
      .get(`/api/projects/${projectId}/customIconCss`)
      .then(response => response.data);
  },
};
