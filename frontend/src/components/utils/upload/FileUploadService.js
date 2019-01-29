import axios from 'axios';

export default {
  upload(url, formData, success, failure) {
    axios.post(url, formData)
      .then(success)
      .catch(failure);
  },
};
