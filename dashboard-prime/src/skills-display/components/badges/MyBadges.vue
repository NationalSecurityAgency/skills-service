<script setup>
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'

const props = defineProps({
  numBadgesCompleted: {
    type: Number,
    required: true
  }
})
const preferences = useSkillsDisplayPreferencesState()
const skillsDisplayInfo = useSkillsDisplayInfo()

</script>

<template>
  <Card class="skills-my-rank w-min-20rem"
        data-cy="myBadges"
        :pt="{ content: { class: 'py-0' } }">
    <template #subtitle>
      <div class="text-center" data-cy="myBadgesTitle">
        My Badges
      </div>
    </template>
    <template #content>
    <span class="fa-stack skills-icon user-rank-stack text-blue-300">
        <i class="fa fa-award fa-stack-2x watermark-icon" />

       <strong class="fa-stack-1x text-primary user-rank-text">
                  {{
           numBadgesCompleted
         }} <span>Badge{{ (numBadgesCompleted > 1 || numBadgesCompleted == 0) ? 's' : '' }}</span>
       </strong>
      </span>
    </template>
    <template #footer v-if="!preferences.isSummaryOnly">
      <router-link
        :to="{ name: skillsDisplayInfo.getContextSpecificRouteName('BadgesDetailsPage') }"
        aria-label="Click to navigate to My Rank page"
        data-cy="myRankBtn">
        <Button
          label="View"
          icon="far fa-eye"
          outlined class="w-full" size="small" />
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