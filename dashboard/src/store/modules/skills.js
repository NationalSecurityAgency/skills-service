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
import SkillsService from '../../components/skills/SkillsService';

const getters = {
  skill(state) {
    return state.skill;
  },
  loadingSkill(state) {
    return state.loadingSkill;
  },
};

const mutations = {
  setSkill(state, value) {
    state.skill = value;
  },
  setLoadingSkill(state, value) {
    state.loadingSkill = value;
  },
};

const actions = {
  loadSkill({ commit }, payload) {
    commit('setLoadingSkill', true);
    return new Promise((resolve, reject) => {
      SkillsService.getSkillDetails(payload.projectId, payload.subjectId, payload.skillId)
        .then((response) => {
          const subjectId = { payload };
          const withSubjId = Object.assign(response, { subjectId });
          commit('setSkill', withSubjId);
          resolve(response);
        })
        .catch((error) => reject(error))
        .finally(() => {
          commit('setLoadingSkill', false);
        });
    });
  },
};

const state = {
  skill: null,
  loadingSkill: false,
};

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
};
