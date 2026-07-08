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
    restoreFocusWithIndicator(element)
    resetElementId()
  }

  /**
   * PrimeVue’s button focus styles are designed around modern accessibility behavior:
   *   :focus = element is focused, regardless of source
   *   :focus-visible = browser thinks a visible focus indicator is appropriate, usually keyboard navigation
   * When you call:
   *   element.focus()
   * the browser may focus the element but not apply :focus-visible, because the focus was caused by script
   * after a mouse/pointer interaction, such as closing a confirmation dialog.
   *
   * To address this issue when we programmatically restore focus, add a temporary class that mimics the PrimeVue focus ring.
   * This requires `st-force-focus-visible` global class to exist (see main.css)
   *
   * This gives sighted keyboard users a visible target after the dialog closes.
   */
  const restoreFocusWithIndicator = (el) => {
    if (!el) {
      return
    }

    el.focus()

    if (!el.matches(':focus-visible')) {
      el.classList.add('st-force-focus-visible')

      const cleanup = () => {
        el.classList.remove('st-force-focus-visible')
        el.removeEventListener('blur', cleanup)
        el.removeEventListener('pointerdown', cleanup)
        el.removeEventListener('keydown', cleanup)
      }

      el.addEventListener('blur', cleanup)
      el.addEventListener('pointerdown', cleanup)
      el.addEventListener('keydown', cleanup)
    }
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