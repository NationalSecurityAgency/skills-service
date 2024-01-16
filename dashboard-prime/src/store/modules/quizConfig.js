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
import QuizService from '../../components/quiz/QuizService'

const getters = {
  quizConfig(state) {
    return state.quizConfig
  },
  loadingQuizConfig(state) {
    return state.loadingQuizConfig
  }
}

const mutations = {
  setQuizConfig(state, value) {
    state.quizConfig = value
  },
  setLoadingQuizConfig(state, value) {
    state.loadingQuizConfig = value
  }
}

const actions = {
  loadQuizConfigState({ commit }, params) {
    const { quizId } = params
    const { updateLoadingVar = true } = params
    return new Promise((resolve, reject) => {
      if (updateLoadingVar) {
        commit('setLoadingQuizConfig', true)
      }
      QuizService.getQuizSettings(quizId)
        .then((response) => {
          if (response && typeof response.reduce === 'function') {
            const quizConfig = response.reduce((map, obj) => {
              // eslint-disable-next-line no-param-reassign
              map[obj.setting] = obj.value
              return map
            }, {})
            commit('setQuizConfig', quizConfig)
          }
          resolve(response)
        })
        .finally(() => {
          if (updateLoadingVar) {
            commit('setLoadingQuizConfig', false)
          }
        })
        .catch((error) => reject(error))
    })
  },
  afterQuizConfigStateLoaded({ state }) {
    return new Promise((resolve) => {
      ;(function waitForQuizConfig() {
        if (!state.loadingQuizConfig) return resolve(state.quizConfig)
        setTimeout(waitForQuizConfig, 100)
        return state.quizConfig
      })()
    })
  }
}

const state = {
  quizConfig: null,
  loadingQuizConfig: true
}

export default {
  namespaced: false,
  state,
  getters,
  mutations,
  actions
}
