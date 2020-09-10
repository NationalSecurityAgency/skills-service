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
import BadgesService from '../../components/badges/BadgesService';
import GlobalBadgeService from '../../components/badges/global/GlobalBadgeService';

const getters = {
  badge(state) {
    return state.badge;
  },
};

const mutations = {
  setBadge(state, value) {
    state.badge = value;
  },
};

const actions = {
  loadBadgeDetailsState({ commit }, payload) {
    return new Promise((resolve, reject) => {
      BadgesService.getBadge(payload.projectId, payload.badgeId)
        .then((response) => {
          commit('setBadge', response);
          resolve(response);
        })
        .catch((error) => reject(error));
    });
  },
  loadGlobalBadgeDetailsState({ commit }, payload) {
    return new Promise((resolve, reject) => {
      GlobalBadgeService.getBadge(payload.badgeId)
        .then((response) => {
          commit('setBadge', response);
          resolve(response);
        })
        .catch((error) => reject(error));
    });
  },
};

const state = {
  badge: null,
};

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
};
