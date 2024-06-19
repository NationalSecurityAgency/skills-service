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
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import { computed } from "vue";

const timeUtils = useTimeUtils();
const props = defineProps(['value']);
const isToday = (timestamp) => {
  return timeUtils.isToday(timestamp);
};

const formattedDate = computed(() => {
  return timeUtils.formatDate(props.value);
})
</script>

<template>
    <span v-if="!value">
      never
    </span>
  <span v-else-if="isToday(value)">
      today
    </span>
  <span v-else>
      {{ formattedDate }}
    </span>
</template>

<style scoped>

</style>