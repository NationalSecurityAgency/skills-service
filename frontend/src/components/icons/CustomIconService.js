import axios from 'axios';

export default {
  getCustomIconCss(projectId) {
    return axios
      .get(`/app/projects/${projectId}/customIconCss`)
      .then(response => response.data);
  },
};
