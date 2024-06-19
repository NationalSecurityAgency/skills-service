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
import { computed, useAttrs } from 'vue'

export const useSkillsInputFallthroughAttributes = () => {
  const attrs = useAttrs()

  const filterAttrs = (attrsToFilter, filterKeys) => {
    return Object.fromEntries(
      Object.entries(attrsToFilter).filter(([key]) => !filterKeys.includes(key))
    )
  }
  const inputAttrs = computed(() => {
    let newAttrs = filterAttrs(attrs, ['class', 'data-cy']);
    if(newAttrs['input-class']) {
      newAttrs.class = newAttrs['input-class'];
      delete newAttrs['input-class'];
    }
    return newAttrs;
  })
  const rootAttrs = computed(() => {
    return filterAttrs(attrs, ['data-cy', 'input-class'])
  })

  return {
    inputAttrs,
    rootAttrs
  }
}