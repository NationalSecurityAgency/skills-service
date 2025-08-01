/*
 * Copyright 2025 SkillTree
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
import {onMounted, onUnmounted, ref, computed} from "vue";

export const useElementSizeUtil = (elementRef) => {
  const width = ref(null)
  const height = ref(null)

  const handleWindowResize = () => {
    const elementRect = elementRef.value?.getBoundingClientRect()
    if (elementRect) {
      width.value = Math.trunc(elementRect.width)
      height.value = Math.trunc(elementRect.height)
    }
  }

  onMounted(() => {
    window.addEventListener('resize', handleWindowResize);
    handleWindowResize()
  })
  onUnmounted(() => {
    window.removeEventListener('resize', handleWindowResize);
  })


  return {
    width,
    height
  }
}