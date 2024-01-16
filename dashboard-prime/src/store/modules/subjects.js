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
import SubjectsService from '../../components/subjects/SubjectsService'

const getters = {
  subject(state) {
    return state.subject
  },
  subjects(state) {
    return state.subjects
  }
}

const mutations = {
  setSubject(state, value) {
    state.subject = value
  },
  setSubjects(state, value) {
    state.subjects = value
  }
}

const actions = {
  loadSubjectDetailsState({ commit }, payload) {
    return new Promise((resolve, reject) => {
      SubjectsService.getSubjectDetails(payload.projectId, payload.subjectId)
        .then((response) => {
          commit('setSubject', response)
          resolve(response)
        })
        .catch((error) => reject(error))
    })
  },
  loadSubjects({ commit }, payload) {
    return new Promise((resolve, reject) => {
      SubjectsService.getSubjects(payload.projectId)
        .then((response) => {
          commit('setSubjects', response)
          resolve(response)
        })
        .catch((error) => reject(error))
    })
  }
}

const state = {
  subject: null,
  subjects: []
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
