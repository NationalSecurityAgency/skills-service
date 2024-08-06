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
import { ref, nextTick } from 'vue'
import { defineStore } from 'pinia'
import { useTimeoutFn } from '@vueuse/core'
import { useLog } from '@/components/utils/misc/useLog.js'
import { useElementHelper } from '@/components/utils/inputForm/UseElementHelper.js'

export const useFocusState = defineStore('focusState', () => {
  const log = useLog()
  const elementHelper = useElementHelper()
  const elementId = ref('')

  function setElementId(newId) {
    log.trace(`setting focus on [${newId}]`)
    elementId.value = newId
  }

  const focusAndReset = (element) => {
    log.trace(`focusing on [${elementId.value}]`)
    element.focus()
    resetElementId()
  }

  function focusOnLastElement() {
    nextTick(() => {
      if (isElementIdPresent()) {
        elementHelper.getElementById(elementId.value).then((element) => {
          if (element) {
            focusAndReset(element)
          } else {
            console.warn(`Failed to focus on [${elementId.value}]`)
          }
        })
      } else {
        log.trace(`No element to focus on [${elementId.value}]`)
      }
    })
  }

  function isElementIdPresent() {
    return elementId.value && elementId.value.trim().length > 0
  }

  function resetElementId() {
    log.trace('reset focus elementId')
    return setElementId('')
  }

  return { elementId, setElementId, focusOnLastElement, isElementIdPresent, resetElementId }

})