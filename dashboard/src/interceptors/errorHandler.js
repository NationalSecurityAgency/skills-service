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
import router from '../router';
import store from '../store/store';

function errorResponseHandler(error) {
  // check if the caller wants to handle the error with displaying the errorPage/dialog
  if (Object.prototype.hasOwnProperty.call(error.config, 'handleError') && error.config.handleError === false) {
    return Promise.reject(error);
  }

  const errorCode = error.response ? error.response.status : undefined;
  if (errorCode === 401) {
    store.commit('clearAuthData');
    const path = window.location.pathname;
    if (path !== '/skills-login') {
      let loginRoute = path !== '/' ? { name: 'Login', query: { redirect: path } } : { name: 'Login' };
      if (store.getters.isPkiAuthenticated) {
        loginRoute = path !== '/' ? { name: 'LandingPage', query: { redirect: path } } : { name: 'LandingPage' };
      }
      router.push(loginRoute);
    }
  } else if (errorCode === 403) {
    let explanation;
    if (error.response && error.response.data && error.response.data.explanation) {
      ({ explanation } = error.response.data);
    }
    router.push({ name: 'NotAuthorizedPage', params: { explanation } });
  } else if (errorCode === 404) {
    let explanation;
    if (error.response && error.response.data && error.response.data.explanation) {
      ({ explanation } = error.response.data);
    }
    router.push({ name: 'NotFoundPage', params: { explanation } });
  } else {
    router.push({ name: 'ErrorPage' });
  }
  return Promise.resolve({ data: {} });
}

// apply interceptor on response
axios.interceptors.response.use(
  (response) => response,
  errorResponseHandler,
);

export default errorResponseHandler;
