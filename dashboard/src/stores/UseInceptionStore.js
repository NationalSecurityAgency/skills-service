/*
 * Copyright 2026 SkillTree
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
import { defineStore } from 'pinia'
import axios from 'axios'
import log from 'loglevel'
import LevelService from '@/components/levels/LevelService.js'

export const useInceptionStore = defineStore('inception', () => {
  const userLevel = ref(null)
  let sessionVersion = 0

  const updateLevel = (level) => {
    if (Number.isInteger(level) && level >= 0) {
      // Reports and the initial level request may complete out of order.
      userLevel.value = Math.max(userLevel.value ?? 0, level)
    }
  }

  const loadUserLevel = () => {
    const requestSession = sessionVersion
    return LevelService.getUserLevel('Inception').then((level) => {
      if (requestSession === sessionVersion) {
        updateLevel(level)
      }
      return level
    })
  }

  const reportSkill = (skillId) => {
    const requestSession = sessionVersion
    const request = axios.post(`/api/projects/Inception/skills/${encodeURIComponent(skillId)}`, { handleError: false }).then(({ data }) => {
      if (requestSession === sessionVersion && data.success) {
        data.completed?.forEach((completion) => {
          if (completion.id === 'OVERALL') {
            updateLevel(completion.level)
          }
        })
      }
      return data
    })
    // Reporting is often fire-and-forget; callers can still handle the rejection.
    request.catch((error) => log.error('Unable to report Inception skill', error))
    return request
  }

  const reset = () => {
    sessionVersion += 1
    userLevel.value = null
  }

  return { userLevel, loadUserLevel, reportSkill, reset }
})
