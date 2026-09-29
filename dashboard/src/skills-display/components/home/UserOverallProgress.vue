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
import {useUserProgressSummaryState} from '@/skills-display/stores/UseUserProgressSummaryState.js'
import CircleProgress from '@/skills-display/components/progress/CircleProgress.vue'
import {useSkillsDisplayThemeState} from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import {computed, onMounted, ref} from 'vue'
import {useNumberFormat} from '@/common-components/filter/UseNumberFormat.js'
import SkillLevel from '@/skills-display/components/progress/MySkillLevel.vue'
import {useSkillsDisplaySubjectState} from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import {useSkillsDisplayAttributesState} from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import VerticalProgressBar from '@/skills-display/components/progress/VerticalProgressBar.vue'
import AchievementCelebration from "@/skills-display/components/progress/celebration/AchievementCelebration.vue";
import {usePluralize} from "@/components/utils/misc/UsePluralize.js";
import {useSkillsDisplayService} from '@/skills-display/services/UseSkillsDisplayService.js'
import {useSkillsDisplayInfo} from '@/skills-display/UseSkillsDisplayInfo.js'

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
const pluralize = usePluralize()
const numFormat = useNumberFormat()
const skillsDisplayService = useSkillsDisplayService()
const skillsDisplayInfo = useSkillsDisplayInfo()
const learningPathProgress = ref({ achieved: 0, total: 0 })

const totalSkills = computed(() => userProgress.value?.totalSkills || 0)
const skillsAchieved = computed(() => userProgress.value?.skillsAchieved || 0)
const skillsPercentAchieved = computed(() => totalSkills.value > 0 ? Math.round((skillsAchieved.value / totalSkills.value) * 100) : 0)
const hasLearningPath = computed(() => learningPathProgress.value.total > 0)
const learningPathPercent = computed(() => Math.round((learningPathProgress.value.achieved / learningPathProgress.value.total) * 100))

const isLevelComplete = computed(() => userProgress.value.levelTotalPoints === -1)
const levelStats = computed(() => {
  return {
    title: isLevelComplete.value ? `${attributes.levelDisplayName} Progress` : `${attributes.levelDisplayName} ${userProgress.value.skillsLevel + 1} Progress`,
    nextLevel: userProgress.value.skillsLevel + 1,
    pointsTillNextLevel: userProgress.value.levelTotalPoints - userProgress.value.levelPoints,
  }
})

onMounted(() => {
  if (!props.isSubject) {
    skillsDisplayService.getSkillDependenciesGraphForProject().then((graph) => {
      if (graph?.edges?.length > 0) {
        const pathNodes = graph.nodes || []
        learningPathProgress.value = {
          achieved: pathNodes.filter((node) => node.achieved).length,
          total: pathNodes.length,
        }
      }
    })
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
              :total-possible-points="userProgress.totalPoints"
              data-cy="overallPoints"
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
            :total-possible-points="userProgress.levelTotalPoints"
            :title="levelStats.title"
            data-cy="levelProgress">
            <template #footer>
              <p v-if="isLevelComplete">All {{ attributes.levelDisplayName.toLowerCase() }}s complete</p>

              <div v-if="!isLevelComplete">
                <div data-cy="pointsTillNextLevelSubtitle">
                  <Tag data-cy="pointsTillNextLevel">{{ numFormat.pretty(levelStats.pointsTillNextLevel) }}</Tag>
                  {{ pluralize.plural(attributes.pointDisplayName, levelStats.pointsTillNextLevel) }} to {{ attributes.levelDisplayName }} {{levelStats.nextLevel }}
                </div>
                <div class="mt-1">
                  You can do it!
                </div>
              </div>
            </template>
          </circle-progress>
        </div>
      </div>
      <div class="mt-9 mx-2 mb-4 flex justify-center sd-theme-achieved-skills-progress" data-cy="achievedSkillsProgress">
        <div class="w-11/12 flex flex-col md:flex-row gap-4 items-stretch">
          <div
            v-if="hasLearningPath"
            class="rounded-lg border border-surface-200 dark:border-surface-700 px-4 py-1 flex items-center gap-3 md:w-96 text-left shadow-sm">
            <circle-progress
              class="shrink-0"
              :diameter="48"
              :total-completed-points="learningPathProgress.achieved"
              :total-possible-points="learningPathProgress.total">
              <template #center>
                <div class="text-xs font-semibold sd-theme-primary-color" data-cy="learningPathPercent">{{ learningPathPercent }}%</div>
              </template>
            </circle-progress>
            <div class="flex-1 min-w-0 self-start">
              <div class="flex items-center gap-2 text-lg font-semibold whitespace-nowrap" data-cy="learningPathTitle">
                <span class="inline-flex w-5 h-5 shrink-0 items-center justify-center" aria-hidden="true">
                  <i class="fas fa-route" />
                </span>
                <span>Learning Path</span>
              </div>
              <div class="text-sm whitespace-nowrap">
                <span class="text-orange-700 dark:text-orange-400 sd-theme-primary-color" data-cy="numAchievedLearningPathItems">{{ learningPathProgress.achieved }}</span> of <span data-cy="numTotalLearningPathItems">{{ learningPathProgress.total }}</span> <span class="ml-0">{{ pluralize.plural("item", learningPathProgress.total) }} achieved</span>
              </div>
            </div>
            <RouterLink
              :to="{ name: skillsDisplayInfo.getContextSpecificRouteName('projectLearningPathPage'), params: { projectId: attributes.projectId } }"
              class="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded border border-green-600 text-green-700 dark:text-green-400 px-3 py-2 text-sm font-medium no-underline hover:bg-green-50 dark:hover:bg-green-950/30"
              data-cy="viewLearningPathLink"
              aria-label="View project learning path">
              <i class="far fa-eye" aria-hidden="true" />
              <span>View</span>
            </RouterLink>
          </div>
          <div class="flex-1 rounded-lg border border-surface-200 dark:border-surface-700 px-4 py-1 flex flex-col shadow-sm text-left">
            <div class="flex items-center gap-2 mb-1" :aria-label="`Achieved ${skillsAchieved} out of ${totalSkills} skills`">
              <div class="flex flex-1 items-center gap-2 text-lg font-semibold" data-cy="achievedSkillsTitle">
                <span class="inline-flex w-5 h-5 shrink-0 items-center justify-center" aria-hidden="true"><i class="fa-solid fa-list-check" /></span>
                <span>Achieved {{ attributes.skillDisplayNamePlural }}</span>
              </div>
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
      </div>
    </template>
  </Card>
  </div>
</template>

<style scoped>

</style>
