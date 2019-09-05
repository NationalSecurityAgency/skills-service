import SkillsConfiguration from '@skills/skills-client-configuration';

import axios from 'axios';
import createAuthRefreshInterceptor from 'axios-auth-refresh';
import router from '@/router';
import store from '@/store';

// eslint-disable-next-line
let service = {};

const refreshAuthorization = (failedRequest) => {
  SkillsConfiguration.setAuthToken(null);
  return service.getAuthenticationToken()
    .then((result) => {
      if (!result.access_token || result.access_token === 'pki') {
        delete axios.defaults.headers.common.Authorization;
      } else {
        SkillsConfiguration.setAuthToken(result.access_token);
        // eslint-disable-next-line no-param-reassign
        failedRequest.response.config.headers.Authorization = `Bearer ${result.access_token}`;
        axios.defaults.headers.common.Authorization = `Bearer ${result.access_token}`;
      }
      return Promise.resolve();
    });
};

// Instantiate the interceptor (you can chain it as it returns the axios instance)
createAuthRefreshInterceptor(axios, refreshAuthorization);

axios.interceptors.response.use(response => response, (error) => {
  if (error.response && error.response.status !== 401) {
    router.push({
      name: 'error',
      params: {
        errorMessage: error.response.statusText,
      },
    });
  }
  return Promise.reject(error);
});

service = {
  getAuthenticationToken() {
    if (!store.state.isAuthenticating) {
      store.commit('isAuthenticating', true);
      if (process.env.NODE_ENV === 'development') {
        this.authenticatingPromise = axios.get(SkillsConfiguration.getAuthenticator());
      } else {
        store.state.parentFrame.emit('needs-authentication');
        this.authenticatingPromise = new Promise((resolve) => {
          const unsubscribe = store.subscribe((mutation) => {
            if (mutation.type === 'authToken') {
              resolve({
                data: {
                  access_token: mutation.payload,
                },
              });
              unsubscribe();
            }
          });
        });
      }
      this.authenticatingPromise
        .finally(() => store.commit('isAuthenticating', false));
    }
    return this.authenticatingPromise;
  },
};

export default service;
