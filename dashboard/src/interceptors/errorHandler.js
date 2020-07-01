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
  console.log('Received Error', error);
  // check if the caller wants to handle the error with displaying the errorPage/dialog
  if (Object.prototype.hasOwnProperty.call(error.config, 'handleError') && error.config.handleError === false) {
    console.log('Caller has set handleError attribute, propagating to the caller', error); // TODO - remove me
    return Promise.reject(error);
  }

  const errorCode = error.response ? error.response.status : undefined;
  console.log(`Received Error with code [${errorCode}]`, error); // TODO - remove me
  if (errorCode === 401) {
    store.commit('clearAuthData');
    const path = window.location.pathname;
    if (path !== '/skills-login') {
      let loginRoute = path !== '/' ? { name: 'Login', query: { redirect: path } } : { name: 'Login' };
      if (store.getters.isPkiAuthenticated) {
        loginRoute = path !== '/' ? { name: 'HomePage', query: { redirect: path } } : { name: 'HomePage' };
      }
      console.log(`401 - re-routing to loginRoute [${loginRoute}], path [${path}]`, error); // TODO - remove me
      router.push(loginRoute);
    }
  } else if (errorCode === 403) {
    console.log('re-routing to NotAuthorizedPage', error); // TODO - remove me
    router.push({ name: 'NotAuthorizedPage' });
  } else {
    console.log('re-routing to ErrorPage', error); // TODO - remove me
    router.push({ name: 'ErrorPage' });
  }
  console.log('rejecting error', error); // TODO - remove me
  return Promise.reject(error).then(result => console.log('resolved', result), result => console.error('rejected', result));
}

// apply interceptor on response
axios.interceptors.response.use(
  response => response,
  errorResponseHandler,
);

export default errorResponseHandler;
