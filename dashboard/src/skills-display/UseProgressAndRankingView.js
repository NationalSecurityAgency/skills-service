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

export const useProgressAndRankingView = ({ projectId, userId, serviceUrl, compactViewDefault, preview }) => {
  const savedChoice = shallowRef(null)
  const storageKey = computed(() => {
    // Never read another trainee's override in an admin preview or create an anonymous preference.
    if (toValue(preview) || !toValue(projectId) || !toValue(userId)) {
      return null
    }
    const service = new URL(toValue(serviceUrl) || window.location.origin, window.location.origin).href.replace(/\/$/, '')
    return `skills-display-compact-view:${[service, toValue(projectId), toValue(userId)].map(encodeURIComponent).join(':')}`
  })

  watch(() => [toValue(projectId), toValue(userId), toValue(serviceUrl), toValue(preview)], (_context, _previous, onCleanup) => {
    const key = storageKey.value
    if (!key) {
      savedChoice.value = ref(null)
      return
    }

    // Dispose the previous binding on context changes; previews never create a storage binding.
    const storageScope = effectScope(true)
    onCleanup(() => storageScope.stop())
    savedChoice.value = storageScope.run(() => useStorage(key, null, undefined, {
      writeDefaults: false,
      flush: 'sync',
      serializer: {
        read: (value) => value === 'true' ? true : value === 'false' ? false : null,
        write: (value) => String(value),
      },
      onError: () => {
        // If iframe storage is blocked, useStorage still retains the in-memory choice.
      },
    }))
  }, { immediate: true, flush: 'sync' })

  const isCompact = computed({
    get: () => savedChoice.value.value ?? (toValue(compactViewDefault) === true),
    set: (value) => {
      savedChoice.value.value = value
    },
  })

  return { isCompact }
}
