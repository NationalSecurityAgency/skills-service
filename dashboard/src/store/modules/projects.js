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
import ProjectsService from '../../components/projects/ProjectService';

const getters = {
  project(state) {
    return state.project;
  },
};

const mutations = {
  setProject(state, value) {
    state.project = value;
  },
};

const actions = {
  loadProjectDetailsState({ commit }, payload) {
    return new Promise((resolve, reject) => {
      ProjectsService.getProjectDetails(payload.projectId)
        .then((response) => {
          commit('setProject', response);
          resolve(response);
        })
        .catch(error => reject(error));
    });
  },
};

const state = {
  project: null,
};

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
};
