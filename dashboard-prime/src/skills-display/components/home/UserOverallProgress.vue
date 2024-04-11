<script setup>
import { useUserProgressSummaryState } from '@/skills-display/stores/UseUserProgressSummaryState.js'
import CircleProgress from '@/skills-display/components/progress/CircleProgress.vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { computed } from 'vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import { useLanguagePluralSupport } from '@/components/utils/misc/UseLanguagePluralSupport.js'
import SkillLevel from '@/skills-display/components/progress/MySkillLevel.vue'

const userProgress = useUserProgressSummaryState()
const themeState = useSkillsDisplayThemeState()
const skillsDisplayPreferences = useSkillsDisplayPreferencesState()
const numFormat = useNumberFormat()
const pluralSupport = useLanguagePluralSupport()

const beforeTodayColor = computed(() => themeState.theme.progressIndicators.beforeTodayColor)
const earnedTodayColor = computed(() => themeState.theme.progressIndicators.earnedTodayColor)
const completeColor = computed(() => themeState.theme.progressIndicators.completeColor)
const incompleteColor = computed(() => themeState.theme.progressIndicators.incompleteColor)
const isLevelComplete = computed(() => userProgress.userProgressSummary.levelTotalPoints === -1)
const levelStats = computed(() => {
  return {
    title: isLevelComplete.value ? `${skillsDisplayPreferences.levelDisplayName} Progress` : `${skillsDisplayPreferences.levelDisplayName} ${userProgress.userProgressSummary.skillsLevel + 1} Progress`,
    nextLevel: userProgress.userProgressSummary.skillsLevel + 1,
    pointsTillNextLevel: userProgress.userProgressSummary.levelTotalPoints - userProgress.userProgressSummary.levelPoints,
  }
})
</script>

<template>
  <Card>
    <template #content>
      <div class="flex text-center">
        <div class="flex-1">
          <div>
            <circle-progress
              :total-completed-points="userProgress.userProgressSummary.points"
              :points-completed-today="userProgress.userProgressSummary.todaysPoints"
              :total-possible-points="userProgress.userProgressSummary.totalPoints"
              :completed-before-today-color="beforeTodayColor"
              :incomplete-color="incompleteColor"
              :total-completed-color="userProgress.userProgressSummary.points === userProgress.userProgressSummary.totalPoints ? completeColor : earnedTodayColor"
              title="Overall Points">
              <template #footer>
                <p v-if="userProgress.userProgressSummary.points > 0 && userProgress.userProgressSummary.points === userProgress.userProgressSummary.totalPoints">All Points earned</p>
                <div v-else>
                  <div>Earn up to <Tag>{{ numFormat.pretty(userProgress.userProgressSummary.totalPoints) }}</Tag> points</div>
                  <div data-cy="overallPointsEarnedToday">
                    <Tag severity="info">{{ numFormat.pretty(userProgress.userProgressSummary.todaysPoints) }}</Tag> Points earned Today
                  </div>
                </div>
              </template>
            </circle-progress>
          </div>

        </div>
        <div class="flex-1">
          <skill-level />
        </div>
        <div class="flex-1">
          <circle-progress
            :total-completed-points="userProgress.userProgressSummary.levelPoints"
            :points-completed-today="userProgress.userProgressSummary.todaysPoints"
            :total-possible-points="userProgress.userProgressSummary.levelTotalPoints"
            :completed-before-today-color="beforeTodayColor"
            :incomplete-color="incompleteColor"
            :total-completed-color="isLevelComplete ? completeColor : earnedTodayColor"
            :title="levelStats.title">
            <template #footer>
              <p v-if="isLevelComplete">All {{ skillsDisplayPreferences.levelDisplayName.toLowerCase() }}s complete</p>

              <div v-if="!isLevelComplete">
                <div>
                  <Tag>{{ numFormat.pretty(levelStats.pointsTillNextLevel) }}</Tag>
                  Point {{ pluralSupport.plural(levelStats.pointsTillNextLevel) }} to {{ skillsDisplayPreferences.levelDisplayName }} {{levelStats.nextLevel }}
                </div>
                <div  data-cy="pointsEarnedTodayForTheNextLevel">
                  <Tag severity="info">{{ numFormat.pretty(userProgress.userProgressSummary.todaysPoints) }}</Tag> Points earned Today
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