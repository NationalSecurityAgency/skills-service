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
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import UserProgressCard from "@/skills-display/components/UserProgressCard.vue";

const props = defineProps({
  badges: {
    type: Object,
    required: true,
  },
})
const attributes = useSkillsDisplayAttributesState()
const skillsDisplayInfo = useSkillsDisplayInfo()

</script>

<template>
  <UserProgressCard title="My Badges" component-name="myBadges" icon="fa fa-award"
                    :route="{ name: skillsDisplayInfo.getContextSpecificRouteName('BadgesDetailsPage') }"
                    :is-summary-only="attributes.isSummaryOnly">
    <template #userRanking>
      <div class="user-rank-text sd-theme-primary-color sd-theme-tile-background-color text-lg py-2">
        <div class="text-3xl font-semibold" style="line-height: 1.2em" data-cy="myBadgesPosition">{{ badges.numBadgesCompleted }}</div>
        <div class="mt-2">out of</div>
        <div><span class="font-semibold">{{ badges.numTotalBadges }}</span> {{ parseInt(badges.numTotalBadges) === 1 ? 'badge' : 'badges' }}</div>
      </div>
    </template>
  </UserProgressCard>
</template>

<style scoped>

</style>