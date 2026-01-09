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
import {useStorage} from "@vueuse/core";

const props = defineProps(['isFullscreen', 'enableZoom', 'enableAnimations', 'horizontalOrientation', 'enableDynamicHeight'])
const emit = defineEmits(['toggleFullscreen', 'toggleZoom', 'toggleAnimations', 'toggleOrientation', 'toggleDynamicHeight'])

const storedEnableZoom = useStorage('learningPath-enableZoom', true);
const storedEnableAnimations = useStorage('learningPath-enableAnimations', true);
const storedDynamicHeight =  useStorage('learningPath-dynamicHeight', false);

const toggleFullscreen = () => {
  emit('toggleFullscreen')
}

const toggleOrientation = () => {
  emit('toggleOrientation');
}

const menu = ref()
const toggleMenu = (event) => {
  menu.value.toggle(event)
}

const toggleZoom = () => {
  emit('toggleZoom')
}

const toggleAnimations = () => {
  emit('toggleAnimations')
}

const toggleDynamicHeight = () => {
  emit('toggleDynamicHeight')
}

</script>

<template>
  <div v-if="isFullscreen">
    <Checkbox
        :defaultValue="storedEnableZoom"
        :value="enableZoom"
        :binary="true"
        @change="toggleZoom"
        inputId="enableZoom"
        data-cy="enableZoom"
        name="enableZoom">
    </Checkbox>
    <span class="align-content-end mr-3">
      <label for="enableZoom" class="font-bold text-primary ml-2">Focus On Select</label>
    </span>
  </div>
  <div v-if="isFullscreen">
    <Checkbox
        :value="enableAnimations"
        :defaultValue="storedEnableAnimations"
        @change="toggleAnimations"
        :binary="true"
        :disabled="!enableZoom"
        inputId="enableAnimations"
        name="enableAnimations">
    </Checkbox>
    <span class="align-content-end mr-3">
      <label for="enableAnimations" class="font-bold text-primary ml-2">Smooth Focus</label>
    </span>
  </div>
  <Button icon="fas fa-rotate"
          severity="info"
          outlined
          raised
          aria-label="Toggle orientation"
          @click="toggleOrientation" />
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
    <div class="p-1">
      <Checkbox
          :value="enableZoom"
          :defaultValue="storedEnableZoom"
          :binary="true"
          @change="toggleZoom"
          inputId="enableZoom"
          data-cy="enableZoom"
          name="enableZoom">
      </Checkbox>
      <span class="align-content-end mr-3">
        <label for="enableZoom" class="font-bold text-primary ml-2">Focus On Select</label>
      </span>
    </div>
    <div class="p-1">
      <Checkbox
          :value="enableAnimations"
          :defaultValue="storedEnableAnimations"
          :binary="true"
          :disabled="!enableZoom"
          @change="toggleAnimations"
          inputId="enableAnimations"
          name="enableAnimations">
      </Checkbox>
      <span class="align-content-end mr-3">
        <label for="enableAnimations" class="font-bold text-primary ml-2">Smooth Focus</label>
      </span>
    </div>
    <div class="p-1">
      <Checkbox
          :value="enableDynamicHeight"
          :defaultValue="storedDynamicHeight"
          :binary="true"
          @change="toggleDynamicHeight"
          inputId="toggleDynamicHeight"
          name="toggleDynamicHeight">
      </Checkbox>
      <span class="align-content-end mr-3">
        <label for="toggleDynamicHeight" class="font-bold text-primary ml-2">Dynamic Height</label>
      </span>
    </div>
  </Popover>
</template>

<style scoped>

</style>