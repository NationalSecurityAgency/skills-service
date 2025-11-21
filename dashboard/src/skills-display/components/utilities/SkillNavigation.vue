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
import {ref} from "vue";
import {useMagicKeys, watchDebounced} from "@vueuse/core";
import {useSkillsDisplayAttributesState} from "@/skills-display/stores/UseSkillsDisplayAttributesState.js";
import {useUserPreferences} from "@/stores/UseUserPreferences.js";
import {useLog} from "@/components/utils/misc/useLog.js";

const props = defineProps(['skill', 'buttonSeverity'])
const emit = defineEmits(['prevButtonClicked', 'nextButtonClicked'])
const attributes = useSkillsDisplayAttributesState()
const keys = useMagicKeys()
const userPreferences = useUserPreferences()
const log = useLog()

const nextButtonShortcut = ref('Ctrl+Alt+N')
const previousButtonShortcut = ref('Ctrl+Alt+P')
userPreferences.afterUserPreferencesLoaded().then((options) => {
  const debounceOptions = { debounce: 250, maxWait: 1000 }
  if (options.sd_next_skill_keyboard_shortcut) {
    nextButtonShortcut.value = options.sd_next_skill_keyboard_shortcut?.toLowerCase().replace(/ /g, '')
  }
  if (options.sd_previous_skill_keyboard_shortcut) {
    previousButtonShortcut.value = options.sd_previous_skill_keyboard_shortcut?.toLowerCase().replace(/ /g, '')
  }

  log.debug(`Next shortcut is : ${nextButtonShortcut.value}`)
  log.debug(`Previous shortcut is : ${previousButtonShortcut.value}`)
  watchDebounced(
      keys[nextButtonShortcut.value],
      () => {
        nextButtonClicked()
      },
      debounceOptions
  )
  watchDebounced(
      keys[previousButtonShortcut.value],
      () => {
        prevButtonClicked()
      },
      debounceOptions
  )
})

const prevButtonClicked = () => {
  emit('prevButtonClicked')
}
const nextButtonClicked = () => {
  emit('nextButtonClicked')
}
</script>

<template>
  <div class="flex-row sm:flex-row items-center flex gap-2 w-full" v-if="skill && (skill.prevSkillId || skill.nextSkillId)">
    <div class="w-28">
      <SkillsButton size="small"
                    outlined
                    id="prevSkillButton"
                    :title="`Previous ${attributes.skillDisplayName} (${previousButtonShortcut})`"
                    class="skills-theme-btn"
                    :severity="buttonSeverity"
                    data-cy="prevSkill"
                    @click="prevButtonClicked"
                    aria-label="previous skill"
                    v-if="skill.prevSkillId">
        <i class="fas fa-arrow-alt-circle-left mr-1" aria-hidden="true"></i> Previous
      </SkillsButton>
    </div>
    <div class="flex-1 text-center " style="font-size: 0.9rem;" data-cy="skillOrder">
      <span class="italic">{{ attributes.skillDisplayName }}</span> <span class="font-semibold">{{ skill.orderInGroup }}</span> <span class="italic">of</span> <span class="font-semibold">{{ skill.totalSkills }}</span>
    </div>
    <div class="w-28 text-right">
      <SkillsButton size="small"
                    outlined
                    id="nextSkillButton"
                    data-cy="nextSkill"
                    :severity="buttonSeverity"
                    class="skills-theme-btn"
                    aria-label="next skill"
                    :title="`Next ${attributes.skillDisplayName} (${nextButtonShortcut})`"
                    @click="nextButtonClicked"
                    v-if="skill.nextSkillId">
        Next <i class="fas fa-arrow-alt-circle-right ml-1" aria-hidden="true"></i>
      </SkillsButton>
    </div>
  </div>
</template>

<style scoped>

</style>