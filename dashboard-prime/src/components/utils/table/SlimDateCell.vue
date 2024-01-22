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
import dayjs from '@/common-components/DayJsCustomizer';

const props = defineProps(['value', 'fromStartOfDay', 'cssClass']);

const fromNow = computed(() => {
  if (props.fromStartOfDay) {
    return dayjs().startOf('day').to(dayjs(props.value));
  }
  return dayjs(props.value).startOf('seconds').fromNow();
})

const isToday = (timestamp) => {
  return dayjs().utc().isSame(dayjs(timestamp), 'day');
};
</script>

<template>
    <span v-if="!value" class="text-primary">
      <Badge variant="warning">Never</Badge>
    </span>
  <span v-else-if="isToday(value)" class="text-primary">
      <Badge variant="info">Today</Badge>
    </span>
  <span v-else class="text-primary small">
      {{ fromNow }}
    </span>
</template>

<style scoped>

</style>
