/*
Copyright 2026 SkillTree

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
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useUserProgressSummaryState } from '@/skills-display/stores/UseUserProgressSummaryState.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import VerticalProgressBar from '@/skills-display/components/progress/VerticalProgressBar.vue'

const props = defineProps({
  userProgress: { type: Object, required: true },
  isSubject: { type: Boolean, default: false },
  badges: { type: Object, default: null },
  learningPathProgress: { type: Object, default: () => ({ achieved: 0, total: 0 }) },
})
const attributes = useSkillsDisplayAttributesState()
const progress = useUserProgressSummaryState()
const displayInfo = useSkillsDisplayInfo()
const numFormat = useNumberFormat()
const route = useRoute()
const percentage = (earned, total) => total > 0 ? Math.min(100, Math.max(0, Math.trunc((earned / total) * 100))) : 0
const overallPercent = computed(() => percentage(props.userProgress.points, props.userProgress.totalPoints))
const hasBadges = computed(() => !props.isSubject && props.badges?.enabled)
const hasLearningPath = computed(() => !props.isSubject && props.learningPathProgress.total > 0)
const learningPathRoute = computed(() => ({ name: displayInfo.getContextSpecificRouteName('projectLearningPathPage'), params: { projectId: attributes.projectId } }))
const metrics = computed(() => {
  const summary = props.userProgress
  const ranking = progress.userRanking
  const allLevelsComplete = summary.levelTotalPoints === -1
  const pointsToNextLevel = Math.max(0, summary.levelTotalPoints - summary.levelPoints)
  const skillsPercent = percentage(summary.skillsAchieved, summary.totalSkills)
  const rankDetailsRoute = {
    name: displayInfo.getContextSpecificRouteName(props.isSubject ? 'subjectRankDetails' : 'myRankDetails'),
    ...(props.isSubject ? { params: { subjectId: route.params.subjectId } } : {}),
  }
  const items = [
    {
      id: 'level', label: `My ${attributes.levelDisplayName}`, icon: 'fas fa-layer-group', iconColor: 'text-fuchsia-700 dark:text-fuchsia-300',
      value: numFormat.pretty(summary.skillsLevel), total: numFormat.pretty(summary.totalLevels),
      allLevelsComplete, nextLevel: `${attributes.levelDisplayName} ${summary.skillsLevel + 1}`,
      detail: allLevelsComplete ? `All ${attributes.levelDisplayNamePlural.toLowerCase()} complete` :
        `${numFormat.pretty(pointsToNextLevel)} ${pointsToNextLevel === 1 ? attributes.pointDisplayNameLower : attributes.pointDisplayNamePlural.toLowerCase()} remaining`,
    },
    {
      id: 'skills', label: `Achieved ${attributes.skillDisplayNamePlural}`, icon: 'fas fa-list-check', iconColor: 'text-indigo-700 dark:text-indigo-300',
      value: numFormat.pretty(summary.skillsAchieved), total: numFormat.pretty(summary.totalSkills),
      percent: skillsPercent,
    },
    {
      id: 'rank', label: 'My Rank', icon: 'fas fa-ranking-star', iconColor: 'text-blue-700 dark:text-blue-300', loading: progress.loadingUserSkillsRanking,
      value: ranking.archivedUser ? 'Archived' : ranking.optedOut ? 'Opted-Out' : `#${numFormat.pretty(ranking.position)}`,
      detail: progress.loadingUserSkillsRanking ? 'Loading your rank' : ranking.archivedUser ? 'Rank unavailable' : ranking.optedOut ?
        `Your position would be #${numFormat.pretty(ranking.position)}` : `of ${numFormat.pretty(ranking.numUsers)} ${ranking.numUsers === 1 ? 'trainee' : 'trainees'}`,
      to: !attributes.isSummaryOnly && !ranking.archivedUser ? rankDetailsRoute : null,
      action: 'Rank details', selector: 'myRankBtn',
    },
  ]
  if (hasBadges.value) {
    items.push({
      id: 'badges', label: 'My Badges', icon: 'fas fa-award', iconColor: 'text-purple-700 dark:text-purple-300',
      value: numFormat.pretty(props.badges.numBadgesCompleted), total: numFormat.pretty(props.badges.numTotalBadges),
      detail: 'Badges earned',
      to: !attributes.isSummaryOnly ? { name: displayInfo.getContextSpecificRouteName('BadgesDetailsPage') } : null,
      action: 'View badges', selector: 'myBadgesBtn',
    })
  }
  return items
})
</script>

<template>
  <Card class="overflow-hidden"
        :pt="{ body: { class: 'p-0!' }, content: { class: 'p-0!' } }" data-cy="compactProgressOverview">
    <template #content>
      <div class="grid lg:grid-rows-[auto_auto_1fr] lg:gap-y-2"
           :class="hasBadges ? 'lg:grid-cols-[minmax(14rem,1fr)_repeat(4,minmax(0,0.75fr))]' : 'lg:grid-cols-[minmax(14rem,1fr)_repeat(3,minmax(0,1fr))]'">
        <div class="border-b border-surface p-4 lg:row-span-3 lg:grid lg:grid-rows-subgrid lg:border-r lg:border-b-0">
          <div class="flex flex-col gap-2 lg:self-start">
            <div class="flex flex-col items-start gap-2">
              <h3 class="flex items-center gap-2 text-xl font-medium sd-theme-primary-color" data-cy="compactOverallTitle">
                <i class="fas fa-chart-line text-orange-700 dark:text-orange-300" aria-hidden="true" />Overall Progress
              </h3>
              <Tag v-if="userProgress.todaysPoints > 0" severity="success" icon="fas fa-arrow-trend-up"
                   :value="`+${numFormat.pretty(userProgress.todaysPoints)} today`" data-cy="compactTodayPoints" />
            </div>
            <div class="flex items-baseline gap-1 font-semibold text-surface-900 dark:text-surface-0 sd-theme-primary-color" data-cy="compactOverallPercent">
              <span class="text-3xl leading-9">{{ overallPercent }}</span><span class="text-xl">%</span>
            </div>
          </div>
          <div class="mt-3 lg:mt-0">
            <vertical-progress-bar :total-progress="overallPercent" :bar-size="6" :disable-daily-color="true"
                                   :aria-label="`${overallPercent}% of available ${attributes.pointDisplayNamePlural.toLowerCase()} earned`" />
          </div>
          <div class="mt-2 text-base lg:mt-0 sd-theme-primary-color">
            <div>
              <span class="font-semibold" data-cy="earnedPoints">{{ numFormat.pretty(userProgress.points) }}</span>
              <span class="mx-1">/</span><span data-cy="totalPoints">{{ numFormat.pretty(userProgress.totalPoints) }}</span>
              {{ attributes.pointDisplayNamePlural.toLowerCase() }}
            </div>
            <div v-if="!userProgress.todaysPoints" class="mt-1 text-base text-muted-color sd-theme-primary-color">0 {{ attributes.pointDisplayNamePlural.toLowerCase() }} earned today</div>
          </div>
        </div>
        <div class="grid grid-cols-2 divide-[var(--p-content-border-color)] md:divide-x lg:contents" :class="hasBadges ? 'md:grid-cols-4' : 'md:grid-cols-3'">
          <div v-for="metric in metrics" :key="metric.id" class="flex min-w-0 flex-col gap-2 p-4 lg:row-span-3 lg:grid lg:grid-rows-subgrid" :aria-busy="metric.loading || undefined"
               :data-cy="`compactMetric-${metric.id}`">
            <div class="flex flex-col gap-2 lg:row-start-1 lg:self-start">
              <h3 class="flex items-start gap-2 text-xl leading-7 font-medium sd-theme-primary-color"
                  :class="{ 'pr-6': metric.id === (hasBadges ? 'badges' : 'rank') }">
                <i :class="[metric.icon, metric.iconColor]" class="mt-1 shrink-0 text-xl" aria-hidden="true" />{{ metric.label }}
              </h3>
              <div class="flex flex-col gap-1">
                <div v-if="metric.loading" class="h-9 w-16 animate-pulse rounded bg-surface-200 dark:bg-surface-700 motion-reduce:animate-none" aria-hidden="true" />
                <div v-else class="flex flex-wrap items-baseline gap-x-1.5 text-surface-900 dark:text-surface-0 sd-theme-primary-color"
                     :data-cy="metric.id === 'skills' ? 'compactAchievedSkillsCount' : undefined">
                  <span class="text-3xl leading-9 font-semibold" :class="{ 'text-xl!': metric.id === 'rank' && (progress.userRanking.optedOut || progress.userRanking.archivedUser) }">{{ metric.value }}</span>
                  <span v-if="metric.total" class="text-base text-muted-color sd-theme-primary-color">/ {{ metric.total }}</span>
                </div>
                <div v-if="metric.id === 'skills'" class="flex items-baseline gap-1 font-semibold text-surface-900 dark:text-surface-0 sd-theme-primary-color" data-cy="compactAchievedSkillsPercent">
                  <span class="text-3xl leading-9">{{ metric.percent }}</span><span class="text-xl">%</span>
                </div>
                <div v-if="metric.id === 'rank' || metric.id === 'badges'" class="text-base text-muted-color sd-theme-primary-color">{{ metric.detail }}</div>
              </div>
            </div>
            <div v-if="metric.id === 'level'" class="mt-auto flex flex-col gap-2 pt-1 lg:row-start-2 lg:row-span-2" data-cy="compactLevelProgress">
              <div v-if="metric.allLevelsComplete" class="flex items-start gap-2 text-base sd-theme-primary-color">
                <i class="fas fa-circle-check mt-1 text-green-700 dark:text-green-300" aria-hidden="true" />
                <span>{{ metric.detail }}</span>
              </div>
              <div v-else class="break-words text-base">
                <div class="font-medium sd-theme-primary-color flex gap-2"><div>Next:</div><Badge severity="secondary">{{ metric.nextLevel }}</Badge></div>
                <div class="mt-1 text-muted-color sd-theme-primary-color">{{ metric.detail }}</div>
              </div>
            </div>
            <div v-else-if="metric.id === 'skills'" class="lg:row-start-2 lg:self-start">
              <vertical-progress-bar :total-progress="metric.percent" :bar-size="6" :disable-daily-color="true"
                                     :aria-label="`${metric.label}: ${metric.percent}% complete`" data-cy="compactAchievedSkillsProgress" />
            </div>
            <div v-else-if="metric.to" class="flex flex-1 flex-col lg:row-start-2 lg:row-span-2">
              <router-link :to="metric.to" :data-cy="metric.selector" :aria-label="metric.action"
                           class="mt-auto inline-flex min-h-6 items-center gap-2 self-start rounded text-base font-medium text-primary hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 sd-theme-primary-color">
                {{ metric.action }}<i class="fas fa-arrow-right" aria-hidden="true" />
              </router-link>
            </div>
          </div>
        </div>
      </div>
      <div v-if="hasLearningPath" class="flex flex-wrap items-center justify-between gap-2 border-t border-surface px-4 py-3 text-base">
        <div class="flex flex-wrap items-center gap-x-3 gap-y-1 sd-theme-primary-color">
          <span class="inline-flex items-center gap-2 font-medium"><i class="fas fa-route text-teal-700 dark:text-teal-300" aria-hidden="true" />Learning Path</span>
          <span><span class="font-semibold">{{ learningPathProgress.achieved }}</span> / {{ learningPathProgress.total }} items achieved</span>
        </div>
        <router-link v-if="!attributes.isSummaryOnly" :to="learningPathRoute" data-cy="viewLearningPathLink"
                     class="inline-flex min-h-6 items-center gap-2 rounded text-base font-medium text-primary hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 sd-theme-primary-color">
          View learning path<i class="fas fa-arrow-right" aria-hidden="true" />
        </router-link>
      </div>
    </template>
  </Card>
</template>
