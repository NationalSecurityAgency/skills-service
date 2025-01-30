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
      <div v-if="optedOut" class="pt-2 text-danger fa-stack-1x user-rank-text sd-theme-primary-color font-bold text-blue-700 text-lg" data-cy="optedOutMessage">
        <div>Opted-Out</div>
        <div style="font-size: 0.8rem; line-height: 1rem;" class="mb-2">
          Your position would be #{{ position }} if you opt-in!
        </div>
      </div>
      <div class="fa-stack-1x user-rank-text sd-theme-primary-color font-bold text-blue-700 text-lg p-1" v-else>
        <div class="text-3xl" style="line-height: 1.2em" data-cy="myRankPosition">#{{ position }}</div>
        <div class="mt-1">out of</div>
        <div>{{ totalUsers }} {{ parseInt(totalUsers) === 1 ? 'user' : 'users' }}</div>
      </div>
    </template>
  </UserProgressCard>
</template>

<style scoped>

</style>