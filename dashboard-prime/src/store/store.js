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
import { createStore } from 'vuex'
import auth from './modules/auth'
import users from './modules/users'
import subjects from './modules/subjects'
import projects from './modules/projects'
import badges from './modules/badges'
import config from './modules/config'
import projConfig from './modules/projConfig'
import libVersion from './modules/libVersion'
import access from './modules/access'
import skills from './modules/skills'
import subjectSkills from './modules/subjectSkills'
import myProgress from './modules/myProgress'
import finalizeInfo from './modules/finalizeInfo'
import quiz from './modules/quiz'
import quizConfig from './modules/quizConfig'

export default createStore({
  state: {
    projectId: '',
    previousUrl: '',
    projectSearch: '',
    showUa: false,
    skillsClientDisplayPath: { path: '/', fromDashboard: false }
  },
  mutations: {
    currentProjectId(state, projectId) {
      state.projectId = projectId
    },
    previousUrl(state, previousUrl) {
      state.previousUrl = previousUrl
    },
    projectSearch(state, projectSearch) {
      state.projectSearch = projectSearch
    },
    showUa(state, showUa) {
      state.showUa = showUa
    },
    skillsClientDisplayPath(state, skillsClientDisplayPath) {
      state.skillsClientDisplayPath = skillsClientDisplayPath
    }
  },
  getters: {
    skillsClientDisplayPath(state) {
      return state.skillsClientDisplayPath
    }
  },
  modules: {
    auth,
    users,
    subjects,
    projects,
    badges,
    config,
    projConfig,
    libVersion,
    access,
    skills,
    subjectSkills,
    myProgress,
    finalizeInfo,
    quiz,
    quizConfig
  }
})
