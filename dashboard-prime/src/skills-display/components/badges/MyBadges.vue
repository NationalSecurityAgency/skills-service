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

const props = defineProps({
  numBadgesCompleted: {
    type: Number,
    required: true
  }
})
const attributes = useSkillsDisplayAttributesState()
const skillsDisplayInfo = useSkillsDisplayInfo()

</script>

<template>
  <Card class="skills-my-rank w-min-15rem h-full"
        data-cy="myBadges"
        :pt="{ content: { class: 'py-0' } }">
    <template #subtitle>
      <div class="text-center text-xl font-medium" data-cy="myBadgesTitle">
        My Badges
      </div>
    </template>
    <template #content>
    <span class="fa-stack skills-icon user-rank-stack text-blue-300">
        <i class="fa fa-award fa-stack-2x watermark-icon" />

       <strong class="fa-stack-1x text-primary user-rank-text sd-theme-primary-color">
                  {{
           numBadgesCompleted
         }} <span>Badge{{ (numBadgesCompleted > 1 || numBadgesCompleted == 0) ? 's' : '' }}</span>
       </strong>
      </span>
    </template>
    <template #footer v-if="!attributes.isSummaryOnly">
      <router-link
        :to="{ name: skillsDisplayInfo.getContextSpecificRouteName('BadgesDetailsPage') }"
        aria-label="Click to navigate to My Rank page"
        data-cy="myBadgesBtn">
        <Button
          label="View"
          icon="far fa-eye"
          outlined class="w-full" size="small" />
      </router-link>
    </template>
  </Card>
</template>

<style scoped>

@media only screen and (min-width: 1200px) {
  .skills-my-rank {
    min-width: 18rem !important;
  }
}

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

.skills-my-rank .skills-icon.user-rank-stack i {
  opacity: 0.38;
}

.skills-my-rank .user-rank-text {
  font-size: 0.5em;
  line-height: 1.2em;
  margin-top: 1.8em;
  background: rgba(255, 255, 255, 0.6);
}
</style>