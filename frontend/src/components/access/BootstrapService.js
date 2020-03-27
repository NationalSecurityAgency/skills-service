import axios from 'axios';

export default {
  grantRoot() {
    return axios.post('/grantFirstRoot').then(response => response.data);
  },
};
