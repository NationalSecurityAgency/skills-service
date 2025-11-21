/*
 * Copyright 2024 SkillTree
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
import { ref } from 'vue'
import axios from 'axios'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { defineStore } from 'pinia'
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";

export const useSkillsDisplayPointHistoryState = defineStore('skillsDisplayPointHistoryState', () => {

  const servicePath = '/api/projects'
  const attributes = useSkillsDisplayAttributesState()
  const appConfig = useAppConfig()

  const pointHistoryMap = ref(new Map())
  const pointHistoryDateLoadedMap = ref(new Map())

  const getUserIdAndVersionParams = () => {
    let config = {}
    if (attributes.userId) {
      config.userId = attributes.userId
    }
    if (appConfig.isPkiAuthenticated) {
      config.idType = 'ID'
    }
    if (attributes.version) {
      config.version = attributes.version
    }
    return config
  }
  const getMapId = (subjectId) => {
    return subjectId || '__skills__overall_points__'
  }

  const loadPointHistory = (subjectId) => {
    const mapId = getMapId(subjectId)
    let url = `${attributes.serviceUrl}${servicePath}/${encodeURIComponent(attributes.projectId)}/subjects/${encodeURIComponent(subjectId)}/pointHistory`
    if (!subjectId) {
      url = `${attributes.serviceUrl}${servicePath}/${encodeURIComponent(attributes.projectId)}/pointHistory`
    }
    const params = getUserIdAndVersionParams()
    params.minNumOfDaysBeforeReturningHistory = 0
    return axios.get(url, {params})
      .then((result) => {
          pointHistoryDateLoadedMap.value.set(mapId, new Date())
          pointHistoryMap.value.set(mapId, result.data)
      })
  }

  const isInitialLoad = (subjectId) => {
    const mapId = getMapId(subjectId)
    return !pointHistoryMap.value.has(mapId)
  }
  const getPointHistory = (subjectId) => {
    const theMap = pointHistoryMap.value
    const mapId = getMapId(subjectId)
    const res = theMap.get(mapId)
    return res
  }

  return {
    loadPointHistory,
    isInitialLoad,
    getPointHistory,
    pointHistoryDateLoadedMap
  }

})