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
import { computed, inject, provide } from 'vue'
import { useSkillsDisplayParentFrameState } from '@/skills-display/stores/UseSkillsDisplayParentFrameState.js'

const chartSnapshotModeKey = Symbol('chartSnapshotMode')

export const provideChartSnapshotMode = (enabled) => provide(chartSnapshotModeKey, enabled)

export const useChartSnapshotMode = () => {
  const localSnapshotMode = inject(chartSnapshotModeKey, false)
  const parentFrame = useSkillsDisplayParentFrameState()
  // Only test harnesses opt in; retain the local setting across child-page navigation.
  return computed(() => localSnapshotMode || (
    parentFrame.options.parentPath?.startsWith('/test-skills-client/') === true &&
    parentFrame.options.disableChartAnimations === true
  ))
}
