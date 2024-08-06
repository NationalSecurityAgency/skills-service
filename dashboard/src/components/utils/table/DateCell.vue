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
import { computed } from 'vue';
import Badge from 'primevue/badge';
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'

const timeUtils = useTimeUtils();

const props = defineProps(['value', 'excludeTime']);

const timeFromNow = computed(() => {
  return timeUtils.timeFromNow(props.value);
})

const formattedDate = computed(() => {
  const formatter = props.excludeTime ? 'YYYY-MM-DD' : 'YYYY-MM-DD HH:mm'
  return timeUtils.formatDate(props.value, formatter);
})
const isToday = (timestamp) => {
  return timeUtils.isToday(timestamp);
};
</script>

<template>
  <div data-cy="dateCell">
    <div>
      <span>{{ formattedDate }}</span>
      <Badge v-if="isToday(value)" severity="info" class="ml-2">Today</Badge>
    </div>
    <div class="font-light text-sm">
      {{ timeFromNow }}
    </div>
  </div>
</template>

<style scoped>

</style>
