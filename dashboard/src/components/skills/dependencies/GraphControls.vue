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
import { ref } from 'vue';
import Popover from "primevue/popover";

const props = defineProps(['isFullscreen'])
const emit = defineEmits(['toggleFullscreen', 'toggleZoom', 'toggleAnimations'])

const enableZoom = ref(true);
const enableAnimations = ref(true)

const toggleFullscreen = () => {
  emit('toggleFullscreen')
}

const menu = ref()
const toggleMenu = (event) => {
  menu.value.toggle(event)
}

const toggleZoom = () => {
  emit('toggleZoom', enableZoom.value)
}

const toggleAnimations = () => {
  emit('toggleAnimations', enableAnimations.value)
}

</script>

<template>
  <div v-if="isFullscreen">
    <SkillsCheckboxInput
        v-model="enableZoom"
        :binary="true"
        @change="toggleZoom"
        inputId="enableZoom"
        data-cy="enableZoom"
        name="enableZoom">
    </SkillsCheckboxInput>
    <span class="align-content-end mr-3">
                    <label for="enableZoom" class="font-bold text-primary ml-2">Focus On Select</label>
                  </span>
  </div>
  <div v-if="isFullscreen">
    <SkillsCheckboxInput
        v-model="enableAnimations"
        @change="toggleAnimations"
        :binary="true"
        inputId="enableAnimations"
        name="enableAnimations">
    </SkillsCheckboxInput>
    <span class="align-content-end mr-3">
                    <label for="enableAnimations" class="font-bold text-primary ml-2">Animations</label>
                  </span>
  </div>
  <Button icon="fas fa-expand"
          severity="info"
          outlined
          raised
          aria-label="Toggle fullscreen"
          @click="toggleFullscreen" />
  <Button
      icon="fas fa-gear"
      severity="info"
      v-if="!isFullscreen"
      outlined
      raised
      data-cy="learningPathSettingsMenu"
      @click="toggleMenu"
      aria-label="Learning Path Settings Button"
      aria-haspopup="true"
      aria-controls="learning_path_settings_menu" />
  <Popover ref="menu">
    <div>
      <SkillsCheckboxInput
          v-model="enableZoom"
          :binary="true"
          @change="toggleZoom"
          inputId="enableZoom"
          data-cy="enableZoom"
          name="enableZoom">
      </SkillsCheckboxInput>
      <span class="align-content-end mr-3">
                    <label for="enableZoom" class="font-bold text-primary ml-2">Focus On Select</label>
                  </span>
    </div>
    <div>
      <SkillsCheckboxInput
          v-model="enableAnimations"
          :binary="true"
          @change="toggleAnimations"
          inputId="enableAnimations"
          name="enableAnimations">
      </SkillsCheckboxInput>
      <span class="align-content-end mr-3">
                    <label for="enableAnimations" class="font-bold text-primary ml-2">Animations</label>
                  </span>
    </div>
  </Popover>
</template>

<style scoped>

</style>