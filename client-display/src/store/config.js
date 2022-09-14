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
import Vue from 'vue';

const getters = {
  config(state) {
    return state.config;
  },
  projectDisplayName(state) {
    return state.config.projectDisplayName || 'Project';
  },
  subjectDisplayName(state) {
    return state.config.subjectDisplayName || 'Subject';
  },
  groupDisplayName(state) {
    return state.config.groupDisplayName || 'Group';
  },
  skillDisplayName(state) {
    return state.config.skillDisplayName || 'Skill';
  },
  levelDisplayName(state) {
    return state.config.levelDisplayName || 'Level';
  },
  displayProjectDescription(state) {
    if (state.config.displayProjectDescription === null || state.config.displayProjectDescription === '') {
      return false;
    }
    return state.config.displayProjectDescription;
  },
};

const mutations = {
  setConfig(state, value) {
    state.config = value;
  },
  setDbUpgradeInProgress(state, value) {
    Vue.set(state.config, 'dbUpgradeInProgress', value);
  },
};

const actions = {
  loadConfigState({ commit, rootState }) {
    const url = rootState.serviceUrl;
    const { projectId } = rootState;
    return new Promise((resolve, reject) => {
      axios.get(`${url}/public/clientDisplay/config?projectId=${projectId}`, {
        withCredentials: false,
      }).then((result) => {
        commit('setConfig', result.data);
        resolve(result.data);
      }).catch((error) => reject(error));
    });
  },
  updateDbUpgradeInProgressIfDifferent({ commit, state }, incomingValue) {
    if (state.config && (state.config.dbUpgradeInProgress === undefined || state.config.dbUpgradeInProgress !== incomingValue)) {
      commit('setDbUpgradeInProgress', incomingValue);
    }
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
