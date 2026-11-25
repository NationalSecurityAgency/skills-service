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
import { computed } from 'vue'

export const useMetricsChartSnapshot = (options, onRendered = () => {}) => {
  // Only a Cypress visit explicitly opting in enables static rendering.
  const enabled = window.Cypress && window.__skillsMetricsChartSnapshot === true
  const snapshotOptions = computed(() => enabled ? { ...options.value, animation: false } : options.value)
  const snapshotPlugins = enabled ? [{
    id: 'metricsSnapshotReady',
    beforeUpdate(chart) {
      chart.canvas.removeAttribute('data-render-ready')
    },
    afterRender(chart) {
      chart.canvas.setAttribute('data-render-ready', 'true')
      onRendered()
    },
    beforeDestroy(chart) {
      chart.canvas.removeAttribute('data-render-ready')
    },
  }] : []
  return { snapshotOptions, snapshotPlugins }
}
