import { useWindowSize } from '@vueuse/core'
import { computed, watch, ref } from 'vue'

export const useResponsiveBreakpoints = () => {

  const windowSize = useWindowSize()

  const currentWidth = ref(windowSize.width)
  watch(() => windowSize.width,
    (newWidth) => {
      currentWidth.value = newWidth
    }
  )

  const sm = computed(() => {
    return currentWidth.value < 576
  })
  const md = computed(() => currentWidth.value < 768)
  const lg = computed(() => currentWidth.value < 992)
  const xl = computed(() => currentWidth.value < 1200)
  const width = computed(() => currentWidth.value)
  return {
    currentWidth,
    width,
    sm,
    md,
    lg,
    xl
  }
}