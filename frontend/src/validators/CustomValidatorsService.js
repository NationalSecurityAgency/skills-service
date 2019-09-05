import axios from 'axios';

export default {
  validateDescription(description) {
    const body = {
      value: description,
    };
    return axios.post('/app/validation/description', body).then(result => result.data);
  },
  validateName(name) {
    const body = {
      value: name,
    };
    return axios.post('/app/validation/name', body).then(result => result.data);
  },
};
