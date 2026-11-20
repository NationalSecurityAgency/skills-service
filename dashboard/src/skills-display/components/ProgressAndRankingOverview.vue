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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Button from 'primevue/button'
import UserOverallProgress from '@/skills-display/components/home/UserOverallProgress.vue'
import MyRank from '@/skills-display/components/rank/MyRank.vue'
import MyBadges from '@/skills-display/components/badges/MyBadges.vue'
import PointProgressChart from '@/skills-display/components/progress/points/PointProgressChart.vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useAuthState } from '@/stores/UseAuthState.js'
import { useUserProgressInitialization } from '@/skills-display/UseUserProgressInitialization.js'
import { useProgressAndRankingView } from '@/skills-display/UseProgressAndRankingView.js'
import { useUserProgressSummaryState } from '@/skills-display/stores/UseUserProgressSummaryState.js'
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import AchievementCelebration from '@/skills-display/components/progress/celebration/AchievementCelebration.vue'
import CompactProgressOverview from '@/skills-display/components/CompactProgressOverview.vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'

const props = defineProps({
  isSubject: { type: Boolean, default: false },
  badges: { type: Object, default: null },
})
const emit = defineEmits(['update:compact'])
const attributes = useSkillsDisplayAttributesState()
const progress = useUserProgressSummaryState()
const subject = useSkillsDisplaySubjectState()
const displayService = useSkillsDisplayService()
const route = useRoute()
const displayInfo = useSkillsDisplayInfo()
const preferencesRoute = { name: 'Preferences', hash: '#progress-view-preference' }
const isEmbedded = displayInfo.isSkillsClientPath()
const userProgress = computed(() => props.isSubject ? subject.subjectSummary : progress.userProgressSummary)
const learningPathProgress = ref({ achieved: 0, total: 0 })
const auth = useAuthState()
const initialization = useUserProgressInitialization()
const { isCompact } = useProgressAndRankingView({
  projectId: () => attributes.projectId,
  userId: () => attributes.userId || auth.userInfo?.userId,
  serviceUrl: () => attributes.serviceUrl,
  pageId: () => route.path,
  compactViewDefault: () => attributes.compactViewDefault,
  compactViewPreference: () => attributes.compactViewPreference,
  preview: initialization?.preview,
  embedded: isEmbedded,
})
const viewToggleLabel = computed(() => isCompact.value ? 'Expand progress view' : 'Collapse progress view')
const showViewPreferenceMessage = ref(false)
watch(() => route.path, () => { showViewPreferenceMessage.value = false })
const toggleProgressView = () => {
  isCompact.value = !isCompact.value
  showViewPreferenceMessage.value = !isEmbedded
}
const hasBadges = computed(() => !props.isSubject && props.badges?.enabled)
const chartHasBeenShown = ref(false)
watch(isCompact, (compact) => {
  emit('update:compact', compact)
  if (!compact) {
    chartHasBeenShown.value = true
  }
}, { immediate: true })
onMounted(() => {
  progress.loadUserSkillsRanking(route.params.subjectId)
  if (!props.isSubject) {
    displayService.getSkillDependenciesGraphForProject().then((graph) => {
      if (graph?.edges?.length > 0) {
        const nodes = graph.nodes || []
        learningPathProgress.value = { achieved: nodes.filter((node) => node.achieved).length, total: nodes.length }
      }
    })
  }
})
</script>

<template>
  <section aria-label="Progress and ranking" data-cy="progressAndRankingOverview" :data-compact="isCompact">
    <achievement-celebration :user-progress="userProgress" />

    <Message v-if="!isEmbedded && showViewPreferenceMessage" severity="info" :closable="true"
             data-cy="progressViewPreferenceMessage" @close="showViewPreferenceMessage = false">
      To permanently change your view preference, visit
      <router-link :to="preferencesRoute" class="font-semibold underline"
                   data-cy="progressViewPreferencesLink">User Preferences</router-link>.
    </Message>

    <div class="relative">
      <div class="absolute right-1 top-1 z-10">
        <Button :icon="isCompact ? 'fas fa-expand-alt' : 'fas fa-compress-alt'"
                severity="primary" outlined size="small"
                class="h-8! w-8! sd-theme-primary-color"
                :aria-label="viewToggleLabel" :title="viewToggleLabel" :aria-expanded="!isCompact"
                data-cy="progressViewExpandCollapseButton" @click="toggleProgressView" />
      </div>
      <compact-progress-overview v-if="isCompact" :user-progress="userProgress" :is-subject="isSubject"
                                 :badges="badges" :learning-path-progress="learningPathProgress" />
      <user-overall-progress v-else :is-subject="isSubject" :learning-path-progress="learningPathProgress" />
    </div>
    <div v-show="!isCompact">
      <div class="mt-4 flex flex-col lg:flex-row items-stretch" :class="isSubject ? 'gap-6' : 'gap-4'">
        <div v-if="!isCompact" :class="{ 'flex items-center': isSubject }">
          <my-rank class="w-full" />
        </div>
        <div class="flex-1 min-w-0">
          <point-progress-chart v-if="chartHasBeenShown" class="w-full" />
        </div>
        <div v-if="!isCompact && hasBadges">
          <my-badges :badges="badges" />
        </div>
      </div>
    </div>
  </section>
</template>
