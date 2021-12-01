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
import SettingService from '../../components/settings/SettingsService';

const getters = {
  projConfig(state) {
    return state.projConfig;
  },
};

const mutations = {
  setProjConfig(state, value) {
    state.projConfig = value;
  },
};

const actions = {
  loadProjConfigState({ commit }, projectId) {
    return new Promise((resolve, reject) => {
      SettingService.getSettingsForProject(projectId)
        .then((response) => {
          if (response) {
            const projConfig = response.reduce((map, obj) => {
              // eslint-disable-next-line no-param-reassign
              map[obj.setting] = obj.value;
              return map;
            }, {});
            commit('setProjConfig', projConfig);
          }
          resolve(response);
        })
        .catch((error) => reject(error));
    });
  },
};

const state = {
  projConfig: null,
};

export default {
  namespaced: false,
  state,
  getters,
  mutations,
  actions,
};
