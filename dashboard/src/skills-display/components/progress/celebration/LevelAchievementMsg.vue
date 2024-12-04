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
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";
import {useSkillsDisplayAttributesState} from "@/skills-display/stores/UseSkillsDisplayAttributesState.js";
import {computed, onMounted} from "vue";
import {useConfettiEffects} from "@/skills-display/components/progress/celebration/UseConfettiEffects.js";
import {useStorage} from "@vueuse/core";
import AchievementMsgContent from "@/skills-display/components/progress/celebration/AchievementMsgContent.vue";
import {useSkillsDisplayThemeState} from "@/skills-display/stores/UseSkillsDisplayThemeState.js";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
import AchievementMsg from "@/skills-display/components/progress/celebration/AchievementMsg.vue";

const props = defineProps({
  userProgress: Object,
})
const isSubject = props.userProgress.subjectId
const timeUtils = useTimeUtils()
const attributes = useSkillsDisplayAttributesState()
const themeState = useSkillsDisplayThemeState()
const confettiEffects = useConfettiEffects()
const appConfig = useAppConfig()

const level = computed(() => props.userProgress.skillsLevel)
const dateAchieved = computed(() => props.userProgress.lastLevelAchieved)
const showLevelCelebration = computed(() => dateAchieved.value && timeUtils.isWithinNDays(dateAchieved.value, 7))
const projectId = computed(() => attributes.projectId)
const storageStartKey = isSubject ? `celebration-proj-${projectId.value}-subject-${props.userProgress.subjectId}-achievement-level${level.value}`: `celebration-proj-${projectId.value}-achievement-level${level.value}`
const confettiAlreadyShown = useStorage(`${storageStartKey}-level${level.value}-confetti-shown`, false)
const disableEncouragementsConfetti = computed(() => themeState.theme.disableEncouragementsConfetti || appConfig.disableEncouragementsConfetti)

onMounted(() => {
  if (showLevelCelebration.value && !confettiAlreadyShown.value && !disableEncouragementsConfetti.value) {
    showConfetti()
    confettiAlreadyShown.value = true
  }
})

const showConfetti = () => {
  if (isSubject) {
    confettiEffects.stars()
  } else {
    confettiEffects.stars2()
  }
}

const currentIcon = computed(() => {
  if (level.value === 1) {
    return 'fas fa-birthday-cake'
  }
  switch (level.value) {
    case 1:
      return 'fas fa-birthday-cake'
    case 2:
      return 'fas fa-gift'
    case 3:
      return 'fas fa-drum'
    case 4:
      return 'fas fa-glass-cheers'
    default:
      return 'fas fa-user-ninja'
  }
})
</script>

<template>
  <div>
    <achievement-msg v-if="showLevelCelebration" :storage-key="storageStartKey" data-cy="levelAchievementCelebrationMsg">
      <template #icon>
        <SkillsButton
            @click="showConfetti"
            text
            aria-label="Show Celebration Confetti"
        >
          <i class="text-4xl" :class="currentIcon" aria-hidden="true"></i>
        </SkillsButton>
      </template>

      <template #content>
        <div v-if="!isSubject">
          <achievement-msg-content v-if="level === 1" title="Level Up!">
            You just crushed
            <Tag severity="info">Level 1</Tag>
            and hit
            <Tag severity="info">Level 2</Tag>
            {{ timeUtils.timeFromNow(dateAchieved) }}!
            <span class="font-italic">Keep that momentum going!</span>
          </achievement-msg-content>
          <achievement-msg-content v-if="level === 2" title="Congratulations!">
            You achieved
            <Tag severity="info">Level 2</Tag>,
            a significant milestone!
            <span class="font-italic">Keep up the fantastic work!</span>
          </achievement-msg-content>
          <achievement-msg-content v-if="level === 3" title="You are on a roll!">
            Reaching
            <Tag severity="info">Level 3</Tag>
            is an impressive achievement!
            <span class="font-italic">Keep pushing forward!</span>
          </achievement-msg-content>
          <achievement-msg-content v-if="level === 4" title="Amazing Job!">
            Achieving <Tag severity="info">Level 4</Tag>
            is a testament to your perseverance and determination!
          </achievement-msg-content>
          <achievement-msg-content v-if="level >= 5" title="Incredible!">
            Congratulation on <Tag severity="info">Level {{ level }}</Tag>!
            You've reached the top!
          </achievement-msg-content>
        </div>
        <div v-if="isSubject">
          <achievement-msg-content v-if="level === 1" title="Subject Level 1 Unlocked!">
            You took the first step in mastering
            <Tag severity="info">{{ props.userProgress.subject }}</Tag>!
            <span class="font-italic">You're off to a fantastic start!!</span>
          </achievement-msg-content>
          <achievement-msg-content v-if="level === 2" title="Subject Level 2 Unlocked!">
            Exciting News! You have reached
            <Tag severity="info">Level 2</Tag>
            in <Tag severity="info">{{ props.userProgress.subject }}</Tag>!
            <span class="font-italic">Keep up the great work!</span>
          </achievement-msg-content>
          <achievement-msg-content v-if="level === 3" title="Subject Level 3 Unlocked!">
            Bravo! You've reached
            <Tag severity="info">Level 3</Tag>
            in <Tag severity="info">{{ props.userProgress.subject }}</Tag>!
            <span class="font-italic">Hard work and dedication are paying off!</span>
          </achievement-msg-content>
          <achievement-msg-content v-if="level === 4" title="Subject Level 4 Unlocked!">
            Well Done! Reaching <Tag severity="info">Level 4</Tag>
            is an impressive achievement!
          </achievement-msg-content>
          <achievement-msg-content v-if="level >= 5" :title="`Subject Level ${level} Unlocked!`">
            Congratulations on reaching
            <Tag severity="info">Level {{ level }}</Tag>
            in
            <Tag severity="info">{{ props.userProgress.subject }}</Tag>!
            Please enjoy this well-deserved recognition!
          </achievement-msg-content>
        </div>
      </template>
    </achievement-msg>

  </div>
</template>

<style scoped>

</style>