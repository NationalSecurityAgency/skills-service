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
import { useTimeoutFn } from '@vueuse/core'
import { useLog } from '@/components/utils/misc/useLog.js'
import { ref } from "vue";

export const useElementHelper = (timeoutInterval = 200, numAttempts = 20) => {
  const log = useLog()
  const elementId = ref('')
  const resolve = ref(null)

  const checkOrWait = (currentAttempt) => {
    const element = document.getElementById(elementId.value)
    if (!element) {
      if (currentAttempt < numAttempts) {
        useTimeoutFn(() => {
          checkOrWait(currentAttempt + 1)
        }, timeoutInterval)
      } else {
        console.warn(`Failed to obtain [${elementId.value}] after trying for [${timeoutInterval*numAttempts}] ms`)
      }
    } else {
      log.trace(`obtained [${elementId.value}]`)
      resolve.value(element);
    }
  }
  const getElementById = (elemId) => {
      return new Promise((res) => {
        elementId.value = elemId
        resolve.value = res
        checkOrWait(0)
      })
  }
  return {
    getElementById,
  }
}