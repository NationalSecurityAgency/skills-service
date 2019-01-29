import axios from 'axios';

const noCacheConfig = { headers: { 'Content-Type': 'application/json', 'Cache-Control': 'no-cache' } };

export default {
  getIconIndex() {
    return axios.get('/icons/custom-icon-index', noCacheConfig).then(response => response.data);
  },
  deleteIcon(iconName, projectId) {
    return axios.delete(`/admin/projects/${projectId}icons/${iconName}`);
  },
};
