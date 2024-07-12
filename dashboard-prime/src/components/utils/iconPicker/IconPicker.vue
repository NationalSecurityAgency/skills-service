/*
Copyright 2020 SkillTree

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
import { ref } from 'vue'
import IconManager from '@/components/utils/iconPicker/IconManager.vue'
import OverlayPanel from 'primevue/overlaypanel'
import { useFocusState } from '@/stores/UseFocusState.js'

const focusState = useFocusState()
const emit = defineEmits(['selected-icon'])

const props = defineProps({
  startIcon: String,
  customIconHeight: {
    type: Number,
    default: 48
  },
  customIconWidth: {
    type: Number,
    default: 48
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const iconManagerOverlayPanel = ref()

const toggleIconDisplay = (event) => {
  iconManagerOverlayPanel.value.toggle(event)
}

const onSelectedIcon = (selectedIcon) => {
  iconManagerOverlayPanel.value.hide()
  emit('selected-icon', selectedIcon)
}

const panelHidden = () => {
  focusState.focusOnLastElement()
}
</script>

<template>
  <div>
    <SkillsButton
      @click="toggleIconDisplay"
      outlined
      :track-for-focus="true"
      class="p-0"
      id="iconPicker"
      role="button"
      aria-roledescription="icon selector button"
      aria-label="icon selector"
      :disabled="disabled"
      data-cy="iconPicker">
      <div class="text-primary text-5xl w-6rem h-5rem flex align-items-center justify-content-center m-0">
        <i :class="[startIcon]" />
      </div>
    </SkillsButton>

    <OverlayPanel ref="iconManagerOverlayPanel" :show-close-icon="true" @hide="panelHidden">
      <icon-manager @selected-icon="onSelectedIcon" name="iconClass"></icon-manager>
    </OverlayPanel>
  </div>
</template>

<style scoped>

</style>
