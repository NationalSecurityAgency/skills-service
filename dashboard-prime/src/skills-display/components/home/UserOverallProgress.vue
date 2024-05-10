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

const beforeTodayColor = computed(() => themeState.theme.progressIndicators.beforeTodayColor)
const earnedTodayColor = computed(() => themeState.theme.progressIndicators.earnedTodayColor)
const completeColor = computed(() => themeState.theme.progressIndicators.completeColor)
const incompleteColor = computed(() => themeState.theme.progressIndicators.incompleteColor)
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
  <Card>
    <template #content>
      <div class="flex flex-column md:flex-row gap-5 align-items-stretch text-center">
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
              title="Overall Points">
              <template #footer>
                <p v-if="userProgress.points > 0 && userProgress.points === userProgress.totalPoints">All Points earned</p>
                <div v-else>
                  <div><Tag data-cy="earnedPoints">{{ numFormat.pretty(userProgress.points) }}</Tag> / <Tag severity="secondary" data-cy="totalPoints">{{ numFormat.pretty(userProgress.totalPoints) }}</Tag> Points</div>
                  <div data-cy="overallPointsEarnedToday">
                    <Tag severity="info" data-cy="pointsEarnedToday">{{ numFormat.pretty(userProgress.todaysPoints) }}</Tag> Points earned Today
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
            data-cy="levelProgress">
            <template #footer>
              <p v-if="isLevelComplete">All {{ attributes.levelDisplayName.toLowerCase() }}s complete</p>

              <div v-if="!isLevelComplete">
                <div data-cy="pointsTillNextLevelSubtitle">
                  <Tag data-cy="pointsTillNextLevel">{{ numFormat.pretty(levelStats.pointsTillNextLevel) }}</Tag>
                  Point{{ pluralSupport.plural(levelStats.pointsTillNextLevel) }} to {{ attributes.levelDisplayName }} {{levelStats.nextLevel }}
                </div>
                <div>
                  You can do it!
                </div>
              </div>
            </template>
          </circle-progress>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>