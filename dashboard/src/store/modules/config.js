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
import SettingsService from '../../components/settings/SettingsService';

const getters = {
  config(state) {
    return state.config;
  },
  isPkiAuthenticated(state) {
    return state.config.authMode === 'PKI';
  },
};

const mutations = {
  setConfig(state, value) {
    state.config = value;
  },
};

const actions = {
  loadConfigState({ commit }) {
    return new Promise((resolve, reject) => {
      SettingsService.getConfig()
        .then((response) => {
          commit('setConfig', response);
          resolve(response);
        })
        .catch(error => reject(error));
    });
  },
};

const state = {
  config: null,
};

export default {
  namespaced: false,
  state,
  getters,
  mutations,
  actions,
};
