<script setup>
import { computed, onMounted } from 'vue'
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import { useUserProgressSummaryState } from '@/skills-display/stores/UseUserProgressSummaryState.js'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useRoute } from 'vue-router'

const preferences = useSkillsDisplayPreferencesState()
const skillsDisplayInfo = useSkillsDisplayInfo()
const progress = useUserProgressSummaryState()
const numberFormat = useNumberFormat()
const route = useRoute()

const position = computed(() => numberFormat.pretty(progress.userRanking.position))
onMounted(() => {
  progress.loadUserSkillsRanking(route.params.subjectId)
})
</script>

<template>
<Card class="skills-my-rank w-min-20rem"
      data-cy="myRank"
      :pt="{ content: { class: 'py-0' } }">
  <template #subtitle>
    <div class="text-center" data-cy="myRankTitle">
      My Rank
    </div>
  </template>
  <template #content>
    <span class="fa-stack skills-icon user-rank-stack text-blue-300">
        <i class="fa fa-users fa-stack-2x watermark-icon" :class="{'text-danger': progress.userRanking.optedOut}"/>
        <strong class="fa-stack-1x text-primary user-rank-text">
          <div v-if="!progress.loadingUserSkillsRanking">
            <div v-if="progress.userRanking.optedOut" class="text-danger">
              <div>Opted-Out</div>
              <div style="font-size: 0.8rem; line-height: 1rem;" class="mb-2">Your position would be <b style="font-size: 0.9rem;" class="badge badge-danger">{{ position }}</b> if you opt-in!</div>
            </div>

            <span v-else>
              <span class="text-blue-700">{{ position }}</span>
            </span>
          </div>
          <skills-spinner :is-loading="progress.loadingUserSkillsRanking"/>
        </strong>
      </span>
  </template>
  <template #footer v-if="!preferences.isSummaryOnly">
    <router-link
      :to="{ name: skillsDisplayInfo.getContextSpecificRouteName('MyRankDetailsPage') }"
      aria-label="Click to navigate to My Rank page"
      data-cy="myRankBtn">
      <Button
        label="View"
        icon="far fa-eye"
        outlined class="w-full" size="small"/>
    </router-link>
  </template>
</Card>
</template>

<style scoped>
.skills-my-rank .skills-icon {
  display: inline-block;
  color: #b1b1b1;
  margin: 5px 0;
}

.skills-my-rank .skills-icon.user-rank-stack {
  margin: 14px 0;
  font-size: 4.1rem;
  width: 100%;
  //color: #0fcc15d1;
}
.skills-my-rank .skills-icon.user-rank-stack i{
  opacity: 0.38;
}

.skills-my-rank .user-rank-text {
  font-size: 0.5em;
  line-height: 1.2em;
  margin-top: 1.8em;
  background: rgba(255, 255, 255, 0.6);
}
</style>