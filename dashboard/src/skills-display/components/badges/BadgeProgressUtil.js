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

const toNumber = (value) => {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

const clampPercent = (value) => Math.min(Math.max(value, 0), 100)

// A single skill's total occurrences are totalPoints / pointIncrement and the
// earned occurrences are points / pointIncrement. Returning occurrences (rather
// than raw points) keeps a skill with a very large point value from dominating
// the overall badge percentage.
const getSkillOccurrences = (skill) => {
  const pointIncrement = toNumber(skill?.pointIncrement)
  if (pointIncrement <= 0) {
    return { total: 0, earned: 0 }
  }

  const total = Math.max(toNumber(skill?.totalPoints) / pointIncrement, 0)
  const earned = Math.min(Math.max(toNumber(skill?.points) / pointIncrement, 0), total)
  return { total, earned }
}

const collectBadgeSkills = (badge) => {
  const skills = []

  if (badge?.projectLevelsAndSkillsSummaries?.length > 0) {
    badge.projectLevelsAndSkillsSummaries.forEach((projectSummary) => {
      if (projectSummary?.skills?.length > 0) {
        skills.push(...projectSummary.skills)
      }
    })
  } else if (badge?.skills?.length > 0) {
    skills.push(...badge.skills)
  }

  return skills
}

// Whole-skill calculation: numSkillsAchieved / numTotalSkills. This is the
// original behavior and is what the Badges summary pages
// (/progress-and-rankings/projects/<proj-id>/badges/<badge-id> and
// /progress-and-rankings/my-badges) continue to use.
export const calculateBadgeCompletionPercentByWholeSkills = (badge) => {
  const totalSkills = toNumber(badge?.numTotalSkills)
  if (totalSkills <= 0) {
    return 0
  }

  const achieved = toNumber(badge?.numSkillsAchieved)
  return Math.trunc(clampPercent((achieved / totalSkills) * 100))
}

// Occurrences calculation: sum every skill's earned occurrences divided by the
// sum of every skill's total occurrences. Used by the Global Badge and Project
// Badge details pages so that partial progress on not-yet-completed skills is
// reflected without letting high-point skills skew the result.
export const calculateBadgeCompletionPercentByOccurrences = (badge) => {
  const skills = collectBadgeSkills(badge)

  let totalOccurrences = 0
  let earnedOccurrences = 0
  skills.forEach((skill) => {
    const { total, earned } = getSkillOccurrences(skill)
    totalOccurrences += total
    earnedOccurrences += earned
  })

  // Fall back to whole-skill counts when there is no usable occurrence data
  // (no skills, or pointIncrement of 0 across the board) so we never divide by
  // zero and still show something sensible.
  if (totalOccurrences <= 0) {
    return calculateBadgeCompletionPercentByWholeSkills(badge)
  }

  return Math.trunc(clampPercent((earnedOccurrences / totalOccurrences) * 100))
}
