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