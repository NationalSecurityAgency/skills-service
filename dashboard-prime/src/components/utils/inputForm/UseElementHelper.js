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