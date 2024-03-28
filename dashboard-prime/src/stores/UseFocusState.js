import { ref, nextTick } from 'vue'
import { defineStore } from 'pinia'
import { useTimeoutFn } from '@vueuse/core'
import { useLog } from '@/components/utils/misc/useLog.js'

export const useFocusState = defineStore('focusState', () => {
  const log = useLog()
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
  const timeoutInterval = 200
  const numAttempts = 20
  const checkOrWait = (currentAttempt) => {
    const element = document.getElementById(elementId.value)
    if (!element) {
      if (currentAttempt < numAttempts) {
        useTimeoutFn(() => {
          checkOrWait(currentAttempt + 1)
        }, timeoutInterval)
      } else {
        console.warn(`Failed to focus on [${elementId.value}] after trying for [${timeoutInterval*numAttempts}] ms`)
      }
    } else {
      focusAndReset(element)
    }
  }

  function focusOnLastElement() {
    nextTick(() => {
      if (isElementIdPresent()) {
        checkOrWait(0)
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