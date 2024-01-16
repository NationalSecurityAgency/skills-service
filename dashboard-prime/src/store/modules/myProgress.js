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
import MyProgressService from '../../components/myProgress/MyProgressService'

const getters = {
  myProjects(state) {
    return state.myProjects
  },
  myProgress(state) {
    return state.myProgress
  }
}

const mutations = {
  setMyProgress(state, value) {
    state.myProgress = value
    state.myProjects = value.projectSummaries
  }
}

const actions = {
  loadMyProgressSummary({ commit }) {
    return new Promise((resolve, reject) => {
      MyProgressService.loadMyProgressSummary()
        .then((response) => {
          commit('setMyProgress', response)
          resolve(response)
        })
        .catch((error) => reject(error))
    })
  }
}

const state = {
  myProjects: null,
  myProgress: null
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
