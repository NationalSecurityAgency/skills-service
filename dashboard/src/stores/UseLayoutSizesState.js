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
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useLayoutSizesState = defineStore('stLayoutSizes', () => {
  const pageHeaderWidth = ref(0)
  const navbarWidth = ref(0)

  const updatePageHeaderWidth = (width) => {
    pageHeaderWidth.value = width
  }
  const updateNavbarWidth = (width) => {
    navbarWidth.value = width
  }

  const oneRem = 16
  const tableMaxWidth = computed(() => {
    return pageHeaderWidth.value - navbarWidth.value - oneRem
  })
  return {
    pageHeaderWidth,
    updatePageHeaderWidth,
    updateNavbarWidth,
    navbarWidth,
    tableMaxWidth
  }
})