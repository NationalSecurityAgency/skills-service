/*
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import axios from 'axios';
import createAuthRefreshInterceptor from 'axios-auth-refresh';
import router from '@/router';
import store from '@/store';

// eslint-disable-next-line
let service = {};

const refreshAuthorization = (failedRequest) => {
  if (store.state.authToken === 'pki') {
    router.push({
      name: 'error',
      params: {
        errorMessage: 'Authentication failed',
      },
    });
    return Promise.reject();
  }
  return service.getAuthenticationToken()
    .then((result) => {
      if (!result.data.access_token || result.data.access_token === 'pki') {
        delete axios.defaults.headers.common.Authorization;
      } else {
        const accessToken = result.data.access_token;
        this.$store.commit('authToken', accessToken);
        // eslint-disable-next-line no-param-reassign
        failedRequest.response.config.headers.Authorization = `Bearer ${accessToken}`;
        axios.defaults.headers.common.Authorization = `Bearer ${accessToken}`;
      }
      return Promise.resolve();
    });
};

// Instantiate the interceptor (you can chain it as it returns the axios instance)
createAuthRefreshInterceptor(axios, refreshAuthorization);

const getErrorMsg = (errorResponse) => {
  let response = '';
  if (!errorResponse || !errorResponse.response || !errorResponse.response.data) {
    return response;
  }

  const { data } = errorResponse.response;
  if (data.error_description) {
    response = data.error_description;
  } else if (data.errorCode && data.explanation) {
    response = data.explanation;
  }

  return response;
};

axios.interceptors.response.use((response) => response, (error) => {
  if (!error || !error.response || (error.response && error.response.status !== 401)) {
    const errorMessage = getErrorMsg(error);
    router.push({
      name: 'error',
      params: {
        errorMessage,
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
        this.authenticatingPromise = axios.get(store.state.authenticator);
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
