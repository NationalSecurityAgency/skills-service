import { ref, nextTick } from 'vue'
import { defineStore } from 'pinia'

export const useFocusState = defineStore('focusState', () => {
  const elementId = ref('')

  function setElementId(newId) {
    elementId.value = newId
  }

  function focusOnLastElement() {
    nextTick(() => {
      if (isElementIdPresent()) {
        const element = document.getElementById(elementId.value);
        if (element) {
          element.focus()
          resetElementId()
        }
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