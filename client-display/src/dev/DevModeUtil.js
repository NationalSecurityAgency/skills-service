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
import TokenReauthorizer from '@/userSkills/service/TokenReauthorizer';

const devModeUtil = {
  isDevelopmentMode() {
    return process.env.NODE_ENV === 'development';
  },
  async configureDevelopmentMode(store) {
    if (!this.isValidDevelopmentMode()) {
      const errorMessage = `
          Development mode is not properly configured
          You must create a local file '.env.development.local' that defines:

          VUE_APP_AUTHENTICATION_URL
          VUE_APP_PROJECT_ID
          VUE_APP_SERVICE_URL

          For an example see .env.development.local.example
        `;

      // eslint-disable-next-line no-alert
      alert(errorMessage);
    } else {
      store.commit('projectId', process.env.VUE_APP_PROJECT_ID);
      store.commit('serviceUrl', process.env.VUE_APP_SERVICE_URL);
      const urlParams = new URLSearchParams(window.location.search);
      if (process.env.VUE_APP_AUTHENTICATION_URL !== 'hydra') {
        let authenticator = process.env.VUE_APP_AUTHENTICATION_URL;
        const loginAsUser = urlParams.get('loginAsUser');
        if (loginAsUser && loginAsUser.length > 0) {
          authenticator = authenticator.replace('user0', loginAsUser);
        }
        store.commit('authenticator', authenticator);
      } else {
        store.commit('authenticator', `${store.state.serviceUrl}/api/projects/${store.state.projectId}/token`);
        // eslint-disable-next-line
        console.log(`Authenticator set to hydra, setting new authenticator to [${store.state.authenticator}]`);
      }
      await this.storeAuthToken(store);
    }
  },
  isValidDevelopmentMode() {
    return process.env.VUE_APP_AUTHENTICATION_URL && process.env.VUE_APP_PROJECT_ID && process.env.VUE_APP_SERVICE_URL;
  },
  async storeAuthToken(store) {
    await TokenReauthorizer.getAuthenticationToken()
      .then((result) => {
        store.commit('authToken', result.data.access_token);
      });
  },
};

export default devModeUtil;
