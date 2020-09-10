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
import UsersService from '../../components/users/UsersService';

const getters = {
  numSkills(state) {
    return state.numSkills;
  },
  userTotalPoints(state) {
    return state.userTotalPoints;
  },
};

const mutations = {
  setNumSkills(state, value) {
    state.numSkills = value;
  },
  setUserTotalPoints(state, value) {
    state.userTotalPoints = value;
  },
  incrementNumSkills(state, value) {
    state.numSkills += value;
  },
  decrementNumSkills(state, value) {
    state.numSkills -= value;
  },
  incrementUserTotalPoints(state, value) {
    state.userTotalPoints += value;
  },
  decrementUserTotalPoints(state, value) {
    state.userTotalPoints -= value;
  },
};

const actions = {
  loadUserDetailsState({ commit }, payload) {
    return new Promise((resolve, reject) => {
      UsersService.getUserSkillsMetrics(payload.projectId, payload.userId)
        .then((response) => {
          commit('setNumSkills', response.numSkills);
          commit('setUserTotalPoints', response.userTotalPoints);
          resolve(response);
        })
        .catch((error) => reject(error));
    });
  },
};

const state = {
  numSkills: 0,
  userTotalPoints: 0,
};

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
};
