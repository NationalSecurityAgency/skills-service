import axios from 'axios';

export default {
  upload(url, formData, success, failure) {
    axios.post(url, formData, { handleError: false })
      .then(success)
      .catch(failure);
  },
};
