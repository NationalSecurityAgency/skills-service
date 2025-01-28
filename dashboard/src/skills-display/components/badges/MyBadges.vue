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
  <UserProgressCard
      title="My Badges"
      component-name="myBadges"
      :user-achieved="badges.numBadgesCompleted"
      :total-available="badges.numTotalBadges"
      :unit="badges.numTotalBadges > 1 ? 'badges' : 'badge'"
      icon="fa fa-award"
      :route="{ name: skillsDisplayInfo.getContextSpecificRouteName('BadgesDetailsPage') }"
      :is-summary-only="attributes.isSummaryOnly" />
</template>

<style scoped>

</style>