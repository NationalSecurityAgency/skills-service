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
import { describe, expect, it } from 'vitest'
import GraphUtils from './GraphUtils.js'

describe('dependency graph tooltips', () => {
  it.each(['<img src=x onerror=alert(1)>', '<svg onload=alert(1)></svg>', '&lt;img src=x onerror=alert(1)&gt;'])('renders API values as literal text: %s', (payload) => {
    const tooltip = GraphUtils.getTitle({
      type: 'Skill', name: payload, skillId: payload, projectId: payload,
      pointIncrement: payload, totalPoints: payload
    }, true)
    expect(tooltip.textContent).toBe(` Cross Project DependencyProject ID: ${payload}Name: ${payload}ID: ${payload}Point Increment: ${payload}Total Points: ${payload}`)
    expect(tooltip.querySelector('img, svg, script, [onerror], [onload]')).toBeNull()
    expect(tooltip.querySelector('.fa-handshake')).not.toBeNull()
    expect(tooltip.querySelectorAll('br')).toHaveLength(6)
  })

  it('renders badge names and contained skill names without creating markup', () => {
    const payload = '<img src=x onerror=alert(1)>'
    const tooltip = GraphUtils.getTitle({ type: 'Badge', name: payload, skillId: 'badge1', containedSkills: [{ name: payload }] }, false)
    expect(tooltip.textContent).toBe(`Name: ${payload}ID: badge1Skills:${payload}`)
    expect(tooltip.querySelector('img')).toBeNull()
  })

  it.each([10, 11, 12])('preserves badge list truncation for %i skills', (count) => {
    const containedSkills = Array.from({ length: count }, (_, i) => ({ name: `Skill ${i}` }))
    const tooltip = GraphUtils.getTitle({ type: 'Badge', name: 'Badge', skillId: 'badge1', containedSkills }, false)
    const names = [...tooltip.querySelectorAll('span')].filter((span) => span.style.padding).map((span) => span.textContent)
    expect(names).toEqual(count > 11 ? [...containedSkills.slice(0, 10).map((skill) => skill.name), `and ${count - 10} more skills...`] : containedSkills.map((skill) => skill.name))
  })
})
