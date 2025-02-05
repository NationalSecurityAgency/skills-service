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

const props = defineProps(['value', 'fromStartOfDay', 'cssClass']);

const fromNow = computed(() => {
  return timeUtils.timeFromNow(props.value, props.fromStartOfDay);
})

const isToday = (timestamp) => {
  return timeUtils.isToday(timestamp);
};
</script>

<template>
    <span v-if="!value" class="text-primary">
      <Badge severity="warn">Never</Badge>
    </span>
  <span v-else-if="isToday(value)" class="text-primary">
      <Badge severity="info">Today</Badge>
    </span>
  <span v-else class="text-primary small">
      {{ fromNow }}
    </span>
</template>

<style scoped>

</style>
