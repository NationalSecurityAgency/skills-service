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

const clampRatio = (value) => Math.min(Math.max(value, 0), 1)

const getSkillCompletionRatio = (skill) => {
  const totalPoints = toNumber(skill?.totalPoints)
  if (totalPoints <= 0) {
    return 0
  }

  return clampRatio(toNumber(skill?.points) / totalPoints)
}

const getLevelCompletionRatio = (projectLevel) => {
  const requiredLevel = toNumber(projectLevel?.requiredLevel)
  if (requiredLevel <= 0) {
    return 0
  }

  return clampRatio(toNumber(projectLevel?.achievedLevel) / requiredLevel)
}

const getProgressRatiosFromProjectSummary = (projectSummary) => {
  const ratios = []

  if (projectSummary?.skills?.length > 0) {
    projectSummary.skills.forEach((skill) => ratios.push(getSkillCompletionRatio(skill)))
  }

  if (projectSummary?.projectLevel) {
    ratios.push(getLevelCompletionRatio(projectSummary.projectLevel))
  }

  return ratios
}

const getFallbackPercent = (badge) => {
  const totalSkills = toNumber(badge?.numTotalSkills)
  if (totalSkills <= 0) {
    return 0
  }

  return Math.trunc(clampRatio(toNumber(badge?.numSkillsAchieved) / totalSkills) * 100)
}

export const calculateBadgeCompletionPercent = (badge) => {
  const dependencyRatios = []

  if (badge?.projectLevelsAndSkillsSummaries?.length > 0) {
    badge.projectLevelsAndSkillsSummaries
      .forEach((projectSummary) => dependencyRatios.push(...getProgressRatiosFromProjectSummary(projectSummary)))
  } else if (badge?.skills?.length > 0) {
    badge.skills.forEach((skill) => dependencyRatios.push(getSkillCompletionRatio(skill)))
  }

  if (dependencyRatios.length === 0) {
    return getFallbackPercent(badge)
  }

  const totalProgress = dependencyRatios.reduce((sum, ratio) => sum + ratio, 0)
  return Math.trunc((totalProgress / dependencyRatios.length) * 100)
}
