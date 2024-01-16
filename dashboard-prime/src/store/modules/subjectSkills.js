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
import dayjs from '@/common-components/DayJsCustomizer'
import SkillsService from '@/components/skills/SkillsService'

const getters = {
  subjectSkills(state) {
    return state.subjectSkills
  },
  loadingSubjectSkills(state) {
    return state.loadingSubjectSkills
  }
}

const mutations = {
  setSubjectSkills(state, value) {
    state.subjectSkills = value
  },
  setLoadingSubjectSkills(state, value) {
    state.loadingSubjectSkills = value
  }
}

const actions = {
  loadSubjectSkills({ commit }, payload) {
    commit('setLoadingSubjectSkills', true)
    return new Promise((resolve, reject) => {
      SkillsService.getSubjectSkills(payload.projectId, payload.subjectId)
        .then((loadedSkills) => {
          const updatedSkills = loadedSkills.map((loadedSkill) => {
            const copy = { ...loadedSkill }
            copy.created = dayjs(loadedSkill.created)
            return copy
          })
          commit('setSubjectSkills', updatedSkills)
          resolve(updatedSkills)
        })
        .catch((error) => reject(error))
        .finally(() => {
          commit('setLoadingSubjectSkills', false)
        })
    })
  }
}

const state = {
  subjectSkills: [],
  loadingSubjectSkills: false
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
