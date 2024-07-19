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
import dayjs from 'dayjs';

const props = defineProps(['options']);
const emit = defineEmits(['time-selected']);

const selectedIndex = ref(0);

const getVariant = (index) => {
  return selectedIndex.value === index ? 'success' : 'secondary';
};

const handleClick = (index) => {
  selectedIndex.value = index;
  const selectedItem = props.options[index];
  const start = dayjs().subtract(selectedItem.length, selectedItem.unit);
  const event = {
    durationLength: selectedItem.length,
    durationUnit: selectedItem.unit,
    startTime: start,
  };
  emit('time-selected', event);
};
</script>

<template>
  <span data-cy="timeLengthSelector" class="time-length-selector">
    <Badge v-for="(item, index) in options" :key="`${item.length}${item.unit}`"
             class="ml-2" :class="{'can-select' : (index !== selectedIndex) }"
             :aria-label="`show data for the last ${item.length} ${item.unit}`"
             :severity="getVariant(index)" @click="handleClick(index)" @keyup.enter="handleClick(index)" tabindex="0">
      {{ item.length }} {{ item.unit }}
    </Badge>
  </span>
</template>

<style scoped>
.can-select {
  cursor: pointer;
}
</style>