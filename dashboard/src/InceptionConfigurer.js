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
import { SkillsConfiguration } from '@skilltree/skills-client-vue';
import store from './store/store';

export default {

  configure() {
    if (store.getters.userInfo && !store.getters.config.needToBootstrap) {
      const projectId = 'Inception';
      const serviceUrl = window.location.origin;
      let authenticator;
      if (store.getters.isPkiAuthenticated) {
        authenticator = 'pki';
      } else {
        authenticator = `/app/projects/${encodeURIComponent(projectId)}/users/${encodeURIComponent(store.getters.userInfo.userId)}/token`;
      }

      SkillsConfiguration.configure({
        serviceUrl,
        projectId,
        authenticator,
      });
    }
  },
};
