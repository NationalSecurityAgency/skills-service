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
import CatalogService from '@/components/skills/catalog/CatalogService'

const getters = {
  finalizeInfo(state) {
    return state.finalizeInfo
  }
}

const mutations = {
  setFinalizeInfo(state, value) {
    state.finalizeInfo = value
  }
}

const actions = {
  loadFinalizeInfo({ commit }, payload) {
    return new Promise((resolve, reject) => {
      CatalogService.getCatalogFinalizeInfo(payload.projectId)
        .then((finalizeInfoRes) => {
          const finalizeInfoUpdated = {
            showFinalizeModal: false,
            finalizeIsRunning: finalizeInfoRes.isRunning,
            finalizeSuccessfullyCompleted: false,
            finalizeCompletedAndFailed: false,
            ...finalizeInfoRes
          }
          commit('setFinalizeInfo', finalizeInfoUpdated)
          resolve(finalizeInfoUpdated)
        })
        .catch((error) => reject(error))
    })
  }
}

const state = {
  finalizeInfo: {
    showFinalizeModal: false,
    finalizeIsRunning: false,
    finalizeSuccessfullyCompleted: false,
    finalizeCompletedAndFailed: false
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
