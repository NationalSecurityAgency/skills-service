/*
 * Copyright 2026 SkillTree
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
import { computed, effectScope, ref, shallowRef, toValue, watch } from 'vue'
import { useStorage } from '@vueuse/core'

// Embedded displays intentionally share a browser choice across projects and learners.
export const embeddedProgressViewStorageKey = 'skills-display:progress-view'

export const useProgressAndRankingView = ({ projectId, userId, serviceUrl, pageId, compactViewDefault, compactViewPreference, preview, embedded }) => {
  // Native and preview toggles belong only to this mounted page.
  const temporaryChoice = ref(null)
  const localChoice = shallowRef(null)
  const usesLocalPreference = computed(() => toValue(embedded) && !toValue(preview))
  watch(usesLocalPreference, (enabled, previous, onCleanup) => {
    localChoice.value = null
    if (enabled) {
      // Dispose storage watchers/listeners when leaving embedded mode or entering preview.
      const storageScope = effectScope()
      onCleanup(() => storageScope.stop())
      localChoice.value = storageScope.run(() => useStorage(embeddedProgressViewStorageKey, null, undefined, {
        writeDefaults: false,
        flush: 'sync',
        serializer: {
          read: (value) => value === 'compact' ? true : value === 'expanded' ? false : null,
          write: (value) => value === true ? 'compact' : value === false ? 'expanded' : '',
        },
        onError: () => {
          // Blocked or full iframe storage must not prevent in-memory toggles.
        },
      }))
    }
  }, { immediate: true, flush: 'sync' })

  watch(() => [toValue(projectId), toValue(userId), toValue(serviceUrl), toValue(pageId), toValue(preview), toValue(embedded)], () => {
    temporaryChoice.value = null
  }, { flush: 'sync' })

  const isCompact = computed({
    get: () => temporaryChoice.value
      ?? (toValue(preview) ? null : usesLocalPreference.value ? localChoice.value?.value : toValue(compactViewPreference))
      ?? (toValue(compactViewDefault) === true),
    set: (value) => {
      if (usesLocalPreference.value) {
        localChoice.value.value = value
      } else {
        temporaryChoice.value = value
      }
    },
  })

  return { isCompact }
}
