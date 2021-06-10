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
import Vue from 'vue';
import Vuex from 'vuex';
import auth from './modules/auth';
import users from './modules/users';
import subjects from './modules/subjects';
import projects from './modules/projects';
import badges from './modules/badges';
import config from './modules/config';
import libVersion from './modules/libVersion';
import access from './modules/access';
import skills from './modules/skills';

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    projectId: '',
    previousUrl: '',
    projectSearch: '',
  },
  mutations: {
    currentProjectId(state, projectId) {
      state.projectId = projectId;
    },
    previousUrl(state, previousUrl) {
      state.previousUrl = previousUrl;
    },
    projectSearch(state, projectSearch) {
      state.projectSearch = projectSearch;
    },
  },
  modules: {
    auth,
    users,
    subjects,
    projects,
    badges,
    config,
    libVersion,
    access,
    skills,
  },
});
