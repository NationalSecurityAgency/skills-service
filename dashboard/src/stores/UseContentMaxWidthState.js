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