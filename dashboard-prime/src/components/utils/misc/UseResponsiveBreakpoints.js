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
import { useWindowSize } from '@vueuse/core'
import { computed, watch, ref } from 'vue'
import ResponsiveBreakpoints from '@/components/utils/misc/ResponsiveBreakpoints.js'

export const useResponsiveBreakpoints = () => {

  const windowSize = useWindowSize()

  const currentWidth = ref(windowSize.width)
  watch(() => windowSize.width,
    (newWidth) => {
      currentWidth.value = newWidth
    }
  )

  // true when less than small (xs)
  const sm = computed(() => {
    return currentWidth.value < ResponsiveBreakpoints.sm
  })
  // md = true when small or smaller
  const md = computed(() => currentWidth.value < ResponsiveBreakpoints.md)
  // lg = true when md or smaller
  const lg = computed(() => currentWidth.value < ResponsiveBreakpoints.lg)
  // xl = true when lg or smaller (anything xl or larger, then all of these are false)
  const xl = computed(() => currentWidth.value < ResponsiveBreakpoints.xl)

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