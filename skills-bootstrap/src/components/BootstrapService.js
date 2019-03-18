import axios from 'axios';

export default {
  registerUser(loginFields) {
    return axios.put('/createRootAccount', loginFields).then(response => response.data);
  },
};
