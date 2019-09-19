import axios from 'axios';

export default {
  getCustomIconCss(projectId, isSupervisor) {
    let url;
    if (projectId) {
      url = `/api/projects/${projectId}/customIconCss`;
    } else if (isSupervisor) {
      url = '/api/icons/customIconCss';
    }
    if (url) {
      return axios
        .get(url)
        .then(response => response.data);
    }
    return new Promise(resolve => resolve(null));
  },
};
