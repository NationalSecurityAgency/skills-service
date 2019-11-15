import axios from 'axios';
import store from '../../store';

function handleFunction(config) {
  const incomingVersion = config.headers['skills-client-lib-version'];
  store.commit('softwareVersion', incomingVersion);
}

// apply interceptor on response
axios.interceptors.response.use(
  (config) => {
    handleFunction(config);
    return config;
  },
);
