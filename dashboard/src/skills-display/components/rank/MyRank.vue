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
import { computed, onMounted } from 'vue'
import { useUserProgressSummaryState } from '@/skills-display/stores/UseUserProgressSummaryState.js'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useRoute } from 'vue-router'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import UserProgressCard from "@/skills-display/components/UserProgressCard.vue";

const attributes = useSkillsDisplayAttributesState()
const skillsDisplayInfo = useSkillsDisplayInfo()
const progress = useUserProgressSummaryState()
const numberFormat = useNumberFormat()
const route = useRoute()

const position = computed(() => numberFormat.pretty(progress.userRanking.position))
const totalUsers = computed(() => numberFormat.pretty(progress.userRanking.numUsers))
const optedOut = computed(() => progress.userRanking.optedOut)
onMounted(() => {
  progress.loadUserSkillsRanking(route.params.subjectId)
})

const toRankDetailsPage = computed(() => {
  if (skillsDisplayInfo.isSubjectPage.value) {
    return { name: skillsDisplayInfo.getContextSpecificRouteName('subjectRankDetails'), params: { subjectId: route.params.subjectId } }
  }

  return { name: skillsDisplayInfo.getContextSpecificRouteName('myRankDetails') }
})
</script>

<template>
  <UserProgressCard title="My Rank" icon="fa fa-users" :route="toRankDetailsPage" :is-summary-only="attributes.isSummaryOnly"
                    component-name="myRank"
                    :loading="progress.loadingUserSkillsRanking">
    <template #userRanking>
      <div class="py-2 sd-theme-primary-color sd-theme-tile-background-color user-rank-text">
        <div v-if="optedOut" data-cy="optedOutMessage">
          <div class="text-2xl">Opted-Out</div>
          <div style="font-size: 0.8rem; line-height: 1rem;" class="mt-2">
            Your position would be <b>#{{ position }}</b> if you opt-in!
          </div>
        </div>
        <div v-else>
          <div class="text-3xl font-semibold" style="line-height: 1.2em" data-cy="myRankPosition">#{{ position }}</div>
          <div class="mt-2">out of</div>
          <div><span class="font-semibold">{{ totalUsers }}</span> {{ parseInt(totalUsers) === 1 ? 'user' : 'users' }}</div>
        </div>
      </div>
    </template>
  </UserProgressCard>
</template>

<style scoped>

</style>