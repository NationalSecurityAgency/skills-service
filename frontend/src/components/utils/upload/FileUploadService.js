import axios from 'axios';

export default {
  upload(url, formData, success, failure) {
    axios.post(url, formData, { headers:{'x-handleError': false } } )
      .then(success)
      .catch(failure);
  },
};
