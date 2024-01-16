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
import QuizService from '@/components/quiz/QuizService'

const getters = {
  quizSummary(state) {
    return state.quizSummary
  },
  loadingQuizSummary(state) {
    return state.loadingQuizSummary
  }
}

const mutations = {
  setQuizSummary(state, value) {
    state.quizSummary = value
  },
  setLoadingQuizSummary(state, value) {
    state.loadingQuizSummary = value
  }
}

const actions = {
  loadQuizSummary({ commit }, payload) {
    return new Promise((resolve, reject) => {
      commit('setLoadingQuizSummary', true)
      QuizService.getQuizDefSummary(payload.quizId)
        .then((response) => {
          commit('setQuizSummary', response)
          resolve(response)
        })
        .catch((error) => reject(error))
        .finally(() => {
          commit('setLoadingQuizSummary', false)
        })
    })
  },
  afterQuizSummaryLoaded({ state }) {
    return new Promise((resolve) => {
      ;(function waitForQuizSummary() {
        if (!state.loadingQuizSummary) return resolve(state.quizSummary)
        setTimeout(waitForQuizSummary, 100)
        return state.quizSummary
      })()
    })
  }
}

const state = {
  quizSummary: null,
  loadingQuizSummary: true
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
