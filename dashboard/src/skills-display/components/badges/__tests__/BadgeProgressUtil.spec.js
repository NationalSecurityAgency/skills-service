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
import { describe, it, expect } from 'vitest'
import {
  calculateBadgeCompletionPercentByOccurrences,
  calculateBadgeCompletionPercentByWholeSkills
} from '../BadgeProgressUtil.js'

// total occurrences = totalPoints / pointIncrement, earned = points / pointIncrement
const skill = (pointIncrement, numPerformToCompletion, performedCount) => ({
  pointIncrement,
  totalPoints: pointIncrement * numPerformToCompletion,
  points: pointIncrement * performedCount
})

describe('calculateBadgeCompletionPercentByOccurrences', () => {
  it('reflects partial progress on a not-yet-completed skill', () => {
    // skill1: 2 occurrences, 1 reported. skill2: 2 occurrences, 0 reported.
    // earned 1 of 4 total occurrences -> 25%
    const badge = {
      skills: [
        skill(100, 2, 1),
        skill(100, 2, 0)
      ]
    }
    expect(calculateBadgeCompletionPercentByOccurrences(badge)).toBe(25)
  })

  it('does not let a high-point skill dominate the overall percentage', () => {
    // Maintainer example: skills worth 100, 50000 and 100 points. With one
    // occurrence each (pointIncrement === totalPoints) every skill is a single
    // occurrence, so completing the two small skills is 2 of 3 occurrences (66%)
    // rather than the ~0.4% that raw points would have produced.
    const badge = {
      skills: [
        { pointIncrement: 100, totalPoints: 100, points: 100 },
        { pointIncrement: 50000, totalPoints: 50000, points: 0 },
        { pointIncrement: 100, totalPoints: 100, points: 100 }
      ]
    }
    expect(calculateBadgeCompletionPercentByOccurrences(badge)).toBe(66)
  })

  it('counts every occurrence of a multi-occurrence high-point skill', () => {
    // The big skill needs 5 occurrences (50000 / 10000). Completing the two
    // single-occurrence skills earns 2 of 7 total occurrences -> 28%.
    const badge = {
      skills: [
        { pointIncrement: 100, totalPoints: 100, points: 100 },
        { pointIncrement: 10000, totalPoints: 50000, points: 0 },
        { pointIncrement: 100, totalPoints: 100, points: 100 }
      ]
    }
    expect(calculateBadgeCompletionPercentByOccurrences(badge)).toBe(28)
  })

  it('returns 100 when every occurrence has been earned', () => {
    const badge = {
      skills: [
        skill(50, 4, 4),
        skill(100, 2, 2)
      ]
    }
    expect(calculateBadgeCompletionPercentByOccurrences(badge)).toBe(100)
  })

  it('aggregates occurrences across global badge project summaries', () => {
    const badge = {
      projectLevelsAndSkillsSummaries: [
        { skills: [skill(100, 2, 1)] },
        { skills: [skill(100, 2, 1)] }
      ]
    }
    // 2 earned of 4 total occurrences -> 50%
    expect(calculateBadgeCompletionPercentByOccurrences(badge)).toBe(50)
  })

  it('falls back to whole skills when pointIncrement is zero', () => {
    const badge = {
      numTotalSkills: 2,
      numSkillsAchieved: 1,
      skills: [
        { pointIncrement: 0, totalPoints: 0, points: 0 },
        { pointIncrement: 0, totalPoints: 0, points: 0 }
      ]
    }
    expect(calculateBadgeCompletionPercentByOccurrences(badge)).toBe(50)
  })

  it('returns 0 for an empty badge', () => {
    expect(calculateBadgeCompletionPercentByOccurrences({})).toBe(0)
    expect(calculateBadgeCompletionPercentByOccurrences(null)).toBe(0)
  })
})

describe('calculateBadgeCompletionPercentByWholeSkills', () => {
  it('uses achieved vs total skill counts', () => {
    expect(calculateBadgeCompletionPercentByWholeSkills({ numTotalSkills: 3, numSkillsAchieved: 1 })).toBe(33)
    expect(calculateBadgeCompletionPercentByWholeSkills({ numTotalSkills: 4, numSkillsAchieved: 4 })).toBe(100)
  })

  it('does not count partial progress', () => {
    // Same setup as the partial-progress occurrences test, but whole skills sees
    // no fully-achieved skill, so the summary view stays at 0.
    const badge = {
      numTotalSkills: 2,
      numSkillsAchieved: 0,
      skills: [skill(100, 2, 1), skill(100, 2, 0)]
    }
    expect(calculateBadgeCompletionPercentByWholeSkills(badge)).toBe(0)
  })

  it('returns 0 when there are no skills', () => {
    expect(calculateBadgeCompletionPercentByWholeSkills({ numTotalSkills: 0 })).toBe(0)
    expect(calculateBadgeCompletionPercentByWholeSkills({})).toBe(0)
  })
})
