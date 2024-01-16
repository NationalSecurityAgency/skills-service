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
import SettingService from '../../components/settings/SettingsService'

const getters = {
  projConfig(state) {
    return state.projConfig
  },
  loadingProjConfig(state) {
    return state.loadingProjConfig
  }
}

const mutations = {
  setProjConfig(state, value) {
    state.projConfig = value
  },
  setLoadingProjConfig(state, value) {
    state.loadingProjConfig = value
  }
}

const actions = {
  loadProjConfigState({ commit }, params) {
    const { projectId } = params
    const { updateLoadingVar = true } = params
    return new Promise((resolve, reject) => {
      if (updateLoadingVar) {
        commit('setLoadingProjConfig', true)
      }
      SettingService.getSettingsForProject(projectId)
        .then((response) => {
          if (response && typeof response.reduce === 'function') {
            const projConfig = response.reduce((map, obj) => {
              // eslint-disable-next-line no-param-reassign
              map[obj.setting] = obj.value
              return map
            }, {})
            commit('setProjConfig', projConfig)
          }
          resolve(response)
        })
        .finally(() => {
          if (updateLoadingVar) {
            commit('setLoadingProjConfig', false)
          }
        })
        .catch((error) => reject(error))
    })
  },
  afterProjConfigStateLoaded({ state }) {
    return new Promise((resolve) => {
      ;(function waitForProjConfig() {
        if (!state.loadingProjConfig) return resolve(state.projConfig)
        setTimeout(waitForProjConfig, 100)
        return state.projConfig
      })()
    })
  }
}

const state = {
  projConfig: null,
  loadingProjConfig: true
}

export default {
  namespaced: false,
  state,
  getters,
  mutations,
  actions
}
