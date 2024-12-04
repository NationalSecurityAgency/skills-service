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

import AchievementMsgContent from "@/skills-display/components/progress/celebration/AchievementMsgContent.vue";
import AchievementMsg from "@/skills-display/components/progress/celebration/AchievementMsg.vue";
import {useConfettiEffects} from "@/skills-display/components/progress/celebration/UseConfettiEffects.js";
import {computed, onMounted} from "vue";
import {useSkillsDisplayInfo} from "@/skills-display/UseSkillsDisplayInfo.js";
import {useStorage} from "@vueuse/core";
import {useSkillsDisplayThemeState} from "@/skills-display/stores/UseSkillsDisplayThemeState.js";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";

const props = defineProps({
  userProgress: Object,
})
const themeState = useSkillsDisplayThemeState()
const confettiEffects = useConfettiEffects()
const appConfig = useAppConfig()
const skillsDisplayInfo = useSkillsDisplayInfo()

const recentlyAchievedBadges = computed(() => props.userProgress.badges?.recentlyAwardedBadges || [])
const showCelebration = computed(() => recentlyAchievedBadges.value?.length > 0)
const storageKey = computed(() => `celebration-proj-${props.userProgress.projectId}-badges-${recentlyAchievedBadges.value.map(b => b.badgeId).sort().join('-')}`)

const confettiAlreadyShown = useStorage(`${storageKey.value}-confetti-shown`, false)
const disableEncouragementsConfetti = computed(() => themeState.theme.disableEncouragementsConfetti || appConfig.disableEncouragementsConfetti)

const showConfetti = () => {
  confettiEffects.stars2()
}
onMounted(() => {
  if (showCelebration.value && !confettiAlreadyShown.value && !disableEncouragementsConfetti.value) {
    showConfetti()
    confettiAlreadyShown.value = true
  }
})

const moreThan1Badge = computed(() => recentlyAchievedBadges.value?.length > 1 )
</script>

<template>
  <achievement-msg v-if="showCelebration" :storage-key="storageKey" data-cy="badgeAchievementCelebrationMsg">
    <template #icon>
      <SkillsButton
          @click="showConfetti"
          text
          aria-label="Show Celebration Confetti"
      >
        <i class="text-4xl fa fa-award " aria-hidden="true"></i>
      </SkillsButton>
    </template>

    <template #content>
      <achievement-msg-content v-if="!moreThan1Badge" title="Badge Achieved!">
        Bravo!
        <router-link
            :to="{ name: skillsDisplayInfo.getContextSpecificRouteName('badgeDetails'), params: { badgeId: recentlyAchievedBadges[0].badgeId } }"
            :data-cy="`badgeLink-${recentlyAchievedBadges[0].badgeId}`">{{ recentlyAchievedBadges[0].badgeName }}</router-link> badge is all yours!
      </achievement-msg-content>
      <achievement-msg-content v-if="moreThan1Badge" title="Badges Achieved!">
        Bravo! You have earned <Tag>{{ recentlyAchievedBadges.length }}</Tag> badges:
          <span v-for="(badge, index) in recentlyAchievedBadges" :key="badge.badgeId">
            <router-link
                :to="{ name: skillsDisplayInfo.getContextSpecificRouteName('badgeDetails'), params: { badgeId: badge.badgeId } }"
                :data-cy="`badgeLink-${badge.badgeId}`">{{ badge.badgeName }}</router-link>
            <span v-if="index !== (recentlyAchievedBadges.length - 1)">, </span>
          </span>.
      </achievement-msg-content>
    </template>
  </achievement-msg>
</template>

<style scoped>

</style>