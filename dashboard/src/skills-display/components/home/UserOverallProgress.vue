/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script setup>
import { useUserProgressSummaryState } from '@/skills-display/stores/UseUserProgressSummaryState.js'
import CircleProgress from '@/skills-display/components/progress/CircleProgress.vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { computed } from 'vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useLanguagePluralSupport } from '@/components/utils/misc/UseLanguagePluralSupport.js'
import SkillLevel from '@/skills-display/components/progress/MySkillLevel.vue'
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import VerticalProgressBar from '@/skills-display/components/progress/VerticalProgressBar.vue'
import AchievementCelebration from "@/skills-display/components/progress/celebration/AchievementCelebration.vue";

const props = defineProps({
  isSubject: {
    type: Boolean,
    default: false,
  }
})

const skillsDisplaySubjectState = useSkillsDisplaySubjectState()
const userProgressSummaryState = useUserProgressSummaryState()
const userProgress = computed(() => {
  return props.isSubject ? skillsDisplaySubjectState.subjectSummary : userProgressSummaryState.userProgressSummary
})
const themeState = useSkillsDisplayThemeState()
const attributes = useSkillsDisplayAttributesState()
const numFormat = useNumberFormat()
const pluralSupport = useLanguagePluralSupport()

const totalSkills = computed(() => userProgress.value?.totalSkills || 0)
const skillsAchieved = computed(() => userProgress.value?.skillsAchieved || 0)
const skillsPercentAchieved = computed(() => totalSkills.value > 0 ? Math.round((skillsAchieved.value / totalSkills.value) * 100) : 0)

const beforeTodayColor = computed(() => themeState.theme.progressIndicators?.beforeTodayColor || '#14a3d2')
const earnedTodayColor = computed(() => themeState.theme.progressIndicators?.earnedTodayColor || '#7ed6f3')
const completeColor = computed(() => themeState.theme.progressIndicators?.completeColor || '#59ad52')
const incompleteColor = computed(() => themeState.theme.progressIndicators?.incompleteColor || '#cdcdcd')
const isLevelComplete = computed(() => userProgress.value.levelTotalPoints === -1)
const levelStats = computed(() => {
  return {
    title: isLevelComplete.value ? `${attributes.levelDisplayName} Progress` : `${attributes.levelDisplayName} ${userProgress.value.skillsLevel + 1} Progress`,
    nextLevel: userProgress.value.skillsLevel + 1,
    pointsTillNextLevel: userProgress.value.levelTotalPoints - userProgress.value.levelPoints,
  }
})
</script>

<template>
  <div>
    <achievement-celebration :user-progress="userProgress"/>
    <Card>
    <template #content>
      <div class="flex flex-col lg:flex-row gap-8 items-stretch text-center">
        <div class="flex-1">
          <div>
            <circle-progress
              :total-completed-points="userProgress.points"
              :points-completed-today="userProgress.todaysPoints"
              :total-possible-points="userProgress.totalPoints"
              :completed-before-today-color="beforeTodayColor"
              :incomplete-color="incompleteColor"
              :total-completed-color="userProgress.points === userProgress.totalPoints ? completeColor : earnedTodayColor"
              data-cy="overallPoints"
              :custom-label="attributes.pointDisplayName"
              :title="`Overall ${ attributes.pointDisplayName }s`">
              <template #footer>
                <p v-if="userProgress.points > 0 && userProgress.points === userProgress.totalPoints">All {{ attributes.pointDisplayName }}s earned</p>
                <div v-else>
                  <div><Tag data-cy="earnedPoints">{{ numFormat.pretty(userProgress.points) }}</Tag> / <Tag severity="secondary" data-cy="totalPoints">{{ numFormat.pretty(userProgress.totalPoints) }}</Tag> {{ attributes.pointDisplayName }}s</div>
                  <div data-cy="overallPointsEarnedToday" class="mt-1">
                    <Tag severity="info" data-cy="pointsEarnedToday">{{ numFormat.pretty(userProgress.todaysPoints) }}</Tag> {{ attributes.pointDisplayName }}s earned Today
                  </div>
                </div>
              </template>
            </circle-progress>
          </div>

        </div>
        <div class="flex-1">
          <skill-level :user-progress="userProgress"/>
        </div>
        <div class="flex-1">
          <circle-progress
            :total-completed-points="userProgress.levelPoints"
            :points-completed-today="userProgress.todaysPoints"
            :total-possible-points="userProgress.levelTotalPoints"
            :completed-before-today-color="beforeTodayColor"
            :incomplete-color="incompleteColor"
            :total-completed-color="isLevelComplete ? completeColor : earnedTodayColor"
            :title="levelStats.title"
            :custom-label="attributes.pointDisplayName"
            data-cy="levelProgress">
            <template #footer>
              <p v-if="isLevelComplete">All {{ attributes.levelDisplayName.toLowerCase() }}s complete</p>

              <div v-if="!isLevelComplete">
                <div data-cy="pointsTillNextLevelSubtitle">
                  <Tag data-cy="pointsTillNextLevel">{{ numFormat.pretty(levelStats.pointsTillNextLevel) }}</Tag>
                  {{ attributes.pointDisplayName }}{{ pluralSupport.plural(levelStats.pointsTillNextLevel) }} to {{ attributes.levelDisplayName }} {{levelStats.nextLevel }}
                </div>
                <div class="mt-1">
                  You can do it!
                </div>
              </div>
            </template>
          </circle-progress>
        </div>
      </div>
      <div class="mt-12 mx-8 flex justify-center" data-cy="achievedSkillsProgress">
        <div class="w-11/12">
        <div class="flex mb-1" :aria-label="`Achieved ${skillsAchieved} out of ${totalSkills} skills`">
          <div class="flex-1 text-lg font-medium">Achieved Skills</div>
          <div><span class="text-orange-700 dark:text-orange-400 font-medium sd-theme-primary-color" data-cy="numAchievedSkills">{{skillsAchieved}}</span> / <span data-cy="numTotalSkills">{{totalSkills}}</span></div>
        </div>
        <vertical-progress-bar
          :total-progress="skillsPercentAchieved"
          :barSize="8"
          :disable-daily-color="true"
          :aria-label="`Achieved ${skillsAchieved} out of ${totalSkills} skills`"
        />
        </div>
      </div>
    </template>
  </Card>
  </div>
</template>

<style scoped>

</style>