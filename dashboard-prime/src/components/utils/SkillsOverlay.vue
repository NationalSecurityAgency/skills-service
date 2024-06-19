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
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';

defineProps({
  show: {
    type: Boolean,
    required: true,
  },
  showSpinner: {
    type: Boolean,
    default: true,
  },
  opacity: {
    type: String,
    default: '100',
  },
});
</script>

<template>
  <BlockUI :blocked="show" :auto-z-index="false"
           :pt:mask:class="`opacity-${opacity}`"
  >
    <slot></slot>
    <div v-if="show" class="text-center overlay-content">
      <slot name="overlay">
        <div v-if="show && showSpinner">
          <SkillsSpinner :is-loading="true"></SkillsSpinner>
        </div>
      </slot>
    </div>
  </BlockUI>
</template>

<style scoped>

.overlay-content {
  position: absolute !important;
  z-index: 100;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%)
}
</style>
