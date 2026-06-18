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
import { calculateBadgeCompletionPercent } from '@/skills-display/components/badges/BadgeProgressUtil.js'

describe('BadgeProgressUtil', () => {
  it('calculates partial progress from required skills', () => {
    const percent = calculateBadgeCompletionPercent({
      skills: [
        { points: 100, totalPoints: 100 },
        { points: 50, totalPoints: 100 }
      ]
    })

    expect(percent).toEqual(75)
  })

  it('calculates partial progress from required project levels', () => {
    const percent = calculateBadgeCompletionPercent({
      projectLevelsAndSkillsSummaries: [
        {
          projectLevel: {
            achievedLevel: 2,
            requiredLevel: 5
          }
        },
        {
          projectLevel: {
            achievedLevel: 2,
            requiredLevel: 5
          }
        }
      ]
    })

    expect(percent).toEqual(40)
  })

  it('combines both skill and level requirements for global badges', () => {
    const percent = calculateBadgeCompletionPercent({
      projectLevelsAndSkillsSummaries: [
        {
          skills: [
            { points: 100, totalPoints: 100 }
          ]
        },
        {
          projectLevel: {
            achievedLevel: 1,
            requiredLevel: 2
          },
          skills: [
            { points: 50, totalPoints: 100 }
          ]
        }
      ]
    })

    expect(percent).toEqual(66)
  })

  it('falls back to legacy achieved skills ratio when details are unavailable', () => {
    const percent = calculateBadgeCompletionPercent({
      numSkillsAchieved: 2,
      numTotalSkills: 5
    })

    expect(percent).toEqual(40)
  })
})
