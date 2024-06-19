/*
 * Copyright 2024 SkillTree
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
import { ref } from 'vue'
import { defineStore } from 'pinia'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useRoute } from 'vue-router'
import GlobalBadgeService from '@/components/badges/global/GlobalBadgeService.js'

export const useSkillsDisplaySubjectState = defineStore('skillDisplaySubjectState', () => {

    const skillsDisplayService = useSkillsDisplayService()
    const route = useRoute()

    const loadingSubjectSummary = ref(true)
    const loadingBadgeSummary = ref(true)
    const subjectSummary = ref({})
    const loadingSkillSummary = ref(true)
    const skillSummary = ref({})

    const loadSubjectSummary = (subjectId, includeSkills = true) => {
      return skillsDisplayService.loadSubjectSummary(subjectId, includeSkills)
        .then((res) => {
          subjectSummary.value = res
          return res
        }).finally(() => {
          loadingSubjectSummary.value = false
        })
    }
  const loadBadgeSummary = (badgeId, isGlobal = false, includeSkills = true) => {
    loadingBadgeSummary.value = true
    const isGlobalAndIndependentOfProject = isGlobal && route.name === 'globalBadgeDetails'

    const dataLoader =  () => isGlobalAndIndependentOfProject ? GlobalBadgeService.getBadge(badgeId) : skillsDisplayService.getBadgeSkills(badgeId, isGlobal, includeSkills)
    return dataLoader()
      .then((badgeSummary) => {
        subjectSummary.value = badgeSummary
        return badgeSummary
      }).finally(() => {
        loadingBadgeSummary.value = false
      })
  }

    const loadSkillSummary = (skillId, optionalCrossProjectId, subjectId) => {
      loadingSkillSummary.value = true
      return skillsDisplayService.getSkillSummary(skillId, optionalCrossProjectId, subjectId)
        .then((res) => {
          skillSummary.value = res
          return res
        }).finally(() => {
          loadingSkillSummary.value = false
        })
    }

    const updateSingleSkillPoints = (skill, pts) => {
      skill.points = skill.points + pts
      skill.todaysPoints = skill.todaysPoints + pts
      if (skill.points === skill.totalPoints) {
        skill.meta.complete = true
        skill.achievedOn = new Date()
      }
    }

    const addPoints = (skillId, pts) => {
      if (pts > 0) {
        if (skillSummary.value?.skillId === skillId) {
          updateSingleSkillPoints(skillSummary.value, pts)
        }
        if (subjectSummary.value?.skills) {
          const foundSkill = findSkillInSubjectSummary(skillId)
          if (foundSkill) {
            updateSingleSkillPoints(foundSkill, pts)

            subjectSummary.value.todaysPoints += pts
            subjectSummary.value.points += pts

            // calculating next level is outside the UIs scope, need to delegate
            if (subjectSummary.value.subjectId) {
              skillsDisplayService.loadSubjectSummary(subjectSummary.value.subjectId, false)
                .then((res) => {
                  subjectSummary.value.skillsLevel = res.skillsLevel
                  subjectSummary.value.levelPoints = res.levelPoints
                  subjectSummary.value.levelTotalPoints = res.levelTotalPoints
                  subjectSummary.value.totalLevels = res.totalLevels
                })
            } else if (subjectSummary.value.badgeId) {
              if (foundSkill.meta.complete) {
                subjectSummary.value.numSkillsAchieved += 1
              }
            }
          }
        }
      }
    }
    const nullifyExpirationDate = (skillId) => {
      let found = false
      if (skillSummary.value?.skillId === skillId) {
        skillSummary.value.expirationDate = null
        found = true
        return
      }
      if (subjectSummary.value?.skills) {
        const foundSkill = subjectSummary.value.skills.find((item) => item.skillId === skillId)
        if (foundSkill) {
          foundSkill.expirationDate = null
          found = true
          return
        }
      }
      if (!found) {
        console.warn(`could not find skill ${skillId}`)
      }
    }

    const findSkillInSubjectSummary = (skillId) => {
      let foundSkill = null;
      for (let i = 0; i < subjectSummary.value.skills.length; i += 1) {
        const skill = subjectSummary.value.skills[i];
        if (skillId === skill.skillId) {
          foundSkill = skill;
        } else if (skill.isSkillsGroupType) {
          foundSkill = skill.children.find((child) => skillId === child.skillId);
        }
        if (foundSkill) {
          break;
        }
      }

      return foundSkill
    }

  const updateDescription = (desc) => {
    const foundSkill = findSkillInSubjectSummary(desc.skillId)
    if (foundSkill) {
      foundSkill.description = desc;
      foundSkill.achievedOn = desc.achievedOn;
      foundSkill.videoSummary = desc.videoSummary;
    }
  }

    return {
      loadSubjectSummary,
      loadingSubjectSummary,
      subjectSummary,
      loadSkillSummary,
      skillSummary,
      loadingSkillSummary,
      addPoints,
      nullifyExpirationDate,
      updateDescription,
      loadingBadgeSummary,
      loadBadgeSummary
    }

  }
)