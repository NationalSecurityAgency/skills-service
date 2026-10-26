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
import { computed, inject, onScopeDispose, provide, ref, watch } from 'vue'
import { useAuthState } from '@/stores/UseAuthState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'

const initializationKey = Symbol('userProgressInitialization')
const refreshIntervalMs = 3000

export const useInitializeUserProgress = (preview) => {
  const attributes = useSkillsDisplayAttributesState()
  const displayService = useSkillsDisplayService()
  const displayInfo = useSkillsDisplayInfo()
  const authState = useAuthState()
  const initialized = ref(false)
  const backfillStatus = ref(null)
  const contextRevision = ref(0)
  const progressRevision = ref(0)
  let requestedRevision = null
  let disposed = false
  onScopeDispose(() => { disposed = true })

  const contextSources = [
    () => attributes.projectId,
    () => attributes.serviceUrl,
    () => attributes.userId,
    () => authState.userInfo?.userId,
    preview
  ]

  watch([...contextSources, () => attributes.loadingConfig], (values, previous = []) => {
    // Configuration readiness controls request timing, but does not create a new display context.
    const contextChanged = contextSources.some((_, index) => values[index] !== previous[index])
    if (contextChanged) {
      contextRevision.value += 1
      initialized.value = false
      backfillStatus.value = null
    }
    const revision = contextRevision.value
    if (attributes.loadingConfig || !attributes.projectId || revision === requestedRevision) {
      return
    }
    requestedRevision = revision
    if (preview.value) {
      initialized.value = true
      return
    }
    displayService.initializeUserProgress().then((result) => {
      if (!disposed && contextRevision.value === revision) {
        backfillStatus.value = result.backfillStatus
        initialized.value = true
      }
    }).catch(() => {
      // Axios handles the error UI; consume the rejection and keep progress unmounted.
    })
  }, { immediate: true })

  const isLoading = computed(() => attributes.loadingConfig || !initialized.value)
  const isProgressPending = computed(() => !isLoading.value && !preview.value && backfillStatus.value === 'PENDING')
  const shouldRefresh = computed(() => isProgressPending.value && displayInfo.isHomePage.value)

  watch([contextRevision, shouldRefresh], ([revision, refresh], previous, onCleanup) => {
    if (!refresh) {
      return
    }
    let active = true
    let timeout
    onCleanup(() => {
      active = false
      clearTimeout(timeout)
    })
    const checkProgress = () => {
      displayService.initializeUserProgress().then((result) => {
        if (!active || contextRevision.value !== revision) {
          return
        }
        if (result.backfillStatus === 'PENDING') {
          timeout = setTimeout(checkProgress, refreshIntervalMs)
        } else {
          backfillStatus.value = result.backfillStatus
          // Remount the page to reload summary, ranking, point history and other progress widgets.
          progressRevision.value += 1
        }
      }).catch(() => {
        // Reuse shared request-error handling and stop refreshing after an error.
      })
    }
    timeout = setTimeout(checkProgress, refreshIntervalMs)
  })

  const state = { isLoading, isProgressPending, contextRevision, progressRevision }
  provide(initializationKey, state)
  return state
}

export const useUserProgressInitialization = () => inject(initializationKey)
