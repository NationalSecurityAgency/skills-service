import axios from 'axios';

export default {
  registerUser(loginFields) {
    return axios.put('/root/createRootAccount', loginFields).then(response => response.data);
  },
  grantRoot() {
    return axios.post('/root/grantFirstRoot').then(response => response.data);
  },
  isLoggedIn() {
    return axios.get('/app/userInfo').then(response => response.data);
  },
  userWithEmailExists(email) {
    return axios.get(`/userExists/${email}`).then(response => !response.data);
  },
};
