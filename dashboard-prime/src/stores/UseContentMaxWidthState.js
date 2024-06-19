import { ref, onMounted, onBeforeUnmount, nextTick, computed, reactive } from 'vue'
import { defineStore } from 'pinia'

export const useContentMaxWidthState = defineStore('contentMaxWidthState', () => {
  const main1ContentWidth = ref(0)
  const navWidth = ref(0)

  const updateWidth = () => {
    main1ContentWidth.value = document.getElementById('mainContent1').clientWidth
    const skillsNavigation = document.getElementById('skillsNavigation')
    const forceReflow = skillsNavigation.offsetWidth
    navWidth.value = forceReflow
  }

  onMounted(() => {
    updateWidth()
    window.addEventListener('resize', updateWidth)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', updateWidth)
  })

  const main2ContentMaxWidth = computed(() => main1ContentWidth.value - navWidth.value - 50)

  const main2ContentMaxWidthStyleObj  = computed(() => {
    return {
      'max-width': `${main2ContentMaxWidth.value}px`
    }
  })

  return {
    updateWidth,
    navWidth,
    main1ContentWidth,
    main2ContentMaxWidth,
    main2ContentMaxWidthStyleObj
  }
})