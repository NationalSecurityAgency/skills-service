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
import {computed, onMounted, watch} from "vue";
import {useStorage} from "@vueuse/core";
import {useSkillsDisplayAttributesState} from "@/skills-display/stores/UseSkillsDisplayAttributesState.js";
import {useSkillsDisplayThemeState} from "@/skills-display/stores/UseSkillsDisplayThemeState.js";
import {useConfettiEffects} from "@/skills-display/components/progress/celebration/UseConfettiEffects.js";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";

const props = defineProps({
  isEnabled: Boolean,
  storageKey: String,
  confettiEffectType: {
    type: String,
    default: 'basic',
    validator: (value) => {
      return ['stars', 'stars2', 'basic'].includes(value)
    }
  },
  icon: String
})
const attributes = useSkillsDisplayAttributesState()
const achievementsCelebrationDisabled = computed(() => attributes.disableAchievementsCelebrations)
const themeState = useSkillsDisplayThemeState()
const confettiEffects = useConfettiEffects()
const appConfig = useAppConfig()

const disableEncouragementsConfetti = computed(() => themeState.theme.disableEncouragementsConfetti || appConfig.disableEncouragementsConfetti)

const msgClosedForGood = useStorage(`${props.storageKey}-closed`, false)
const confettiAlreadyShown = useStorage(`${props.storageKey}-confetti-shown`, false)

const close = () => {
  msgClosedForGood.value = true
}
const showConfetti = () => {
  if (shouldShowMsg.value && shouldShowConfetti.value) {
    doShowConfetti()
    confettiAlreadyShown.value = true
  }
}

const doShowConfetti = () => {
  if (props.confettiEffectType === 'stars') {
    confettiEffects.stars()
  } else if (props.confettiEffectType === 'stars2') {
    confettiEffects.stars2()
  } else {
    confettiEffects.simpleConfetti()
  }
}

const shouldShowMsg = computed(() => props.isEnabled && !msgClosedForGood.value && !achievementsCelebrationDisabled.value)
const shouldShowConfetti = computed(() => !confettiAlreadyShown.value && !disableEncouragementsConfetti.value)
onMounted(() => {
  showConfetti()
})

watch(
    () => props.isEnabled,
    (newIsEnabled, oldIsEnabled) => {
      if (!oldIsEnabled && newIsEnabled) {
        showConfetti()
      }
    }
)

</script>

<template>
  <Message v-if="shouldShowMsg" severity="success">
    <template #container>
      <div class="px-2 py-4 flex items-center gap-4">
        <div>
          <SkillsButton
              @click="doShowConfetti"
              text
              aria-label="Show Celebration Confetti"
          >
            <i class="text-4xl" :class="icon" aria-hidden="true"></i>
          </SkillsButton>
        </div>
        <div class="text-center text-xl flex-1">
          <slot name="content"/>
        </div>
        <div>
          <SkillsButton
              @click="close"
              class="text-xl"
              icon="fas fa-times"
              text
              data-cy="closeCelebrationMsgBtn"
              aria-label="Close celebration message"
          />
        </div>

      </div>
    </template>
  </Message>
</template>

<style scoped>

</style>