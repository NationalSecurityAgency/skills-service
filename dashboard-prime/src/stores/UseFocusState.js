import { ref, nextTick } from 'vue'
import { defineStore } from 'pinia'
import { useTimeoutFn } from '@vueuse/core'

export const useFocusState = defineStore('focusState', () => {
  const elementId = ref('')

  function setElementId(newId) {
    elementId.value = newId
  }

  const focusAndReset = (element) => {
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
      }
    })
  }

  function isElementIdPresent() {
    return elementId.value && elementId.value.trim().length > 0
  }

  function resetElementId() {
    return setElementId('')
  }

  return { elementId, setElementId, focusOnLastElement, isElementIdPresent, resetElementId }

})