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
import { computed } from 'vue';

const props = defineProps(['type']);
const types = [
  'Overall',
  'Subject',
  'Skill',
  'Badge',
];

const isUnknownType = computed(() => {
  return types.find((item) => item === props.type) === undefined;
});
</script>

<template>
  <div style="vertical-align: text-top !important;">
    <span class="border-info rounded-sm d-inline-block p-1 text-center icon text-secondary">
      <span v-if="type === types[0]">
        <i class="fa fa-trophy"/>
      </span>
      <span v-if="type === types[1]">
            <i class="fa fa-cubes "/>
      </span>
      <span v-if="type === types[2]">
            <i class="fa fa-graduation-cap "/>
      </span>
      <span v-if="type === types[3]">
            <i class="fa fa-award "/>
      </span>
      <span v-if="isUnknownType">
            <i class="fa fa-award "/>
      </span>
    </span>
    <span style="vertical-align: text-top !important;">
      {{ type }}
    </span>
  </div>
</template>

<style scoped>

</style>