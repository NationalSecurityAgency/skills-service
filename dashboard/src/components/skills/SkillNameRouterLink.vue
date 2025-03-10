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

import { computed, onMounted, ref } from 'vue';
import StringHighlighter from '@/common-components/utilities/StringHighlighter.js';

const props = defineProps({
  skill: Object,
  subjectId: String,
  filterValue: String,
  readOnly: {
    type: Boolean,
    required: false,
    default: false,
  }
});

const slop = ref(15);
const displayFullText = ref(false);

onMounted(() => {
  displayFullText.value = props.skill.name.length < props.limit + slop.value;
});


const toDisplay = computed(() => {
  if (displayFullText.value) {
    return `${props.skill.name}`;
  }
  return `${props.skill.name.substring(0, props.limit)}`;
});

const highlightedValue = computed(() => {
  const value = toDisplay.value;
  const filterValue = props.filterValue ? props.filterValue.trim() : '';
  if (filterValue && filterValue.length > 0) {
    const highlighted = StringHighlighter.highlight(value, filterValue);
    return highlighted || value;
  } else {
    return value;
  }
});

</script>

<template>
  <div class="">
    <router-link :data-cy="`manageSkillLink_${skill.skillId}`"
                 :to="{ name:'SkillOverview', params: { projectId: skill.projectId, subjectId: subjectId, skillId: skill.skillId }}"
                 :aria-label="`${readOnly ? 'View' : 'Manage'} skill ${skill.name} via link`">
      <div data-cy="highlightedValue"
           class="text-lg overflow-hidden text-ellipsis"
           style="overflow-wrap: anywhere;"
           :title="toDisplay?.length > 30 ? toDisplay : ''"
           v-html="highlightedValue" />
    </router-link>
  </div>
</template>

<style scoped>
</style>