import axios from 'axios';

export default {
  getCustomIconCss(projectId) {
    let url = `/api/projects/${projectId}/customIconCss`;
    if (!projectId) {
      url = '/supervisor/customIconCss';
    }
    return axios
      .get(url)
      .then(response => response.data);
  },
};
