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
import { ref, onMounted } from 'vue';
import { useColors } from '@/skills-display/components/utilities/UseColors.js'

const props = defineProps({
  selfReportStats: Array,
});
const colors = useColors()

const cards = ref([{
  id: 'Approval',
  label: 'Approval Required',
  count: 0,
  icon: 'far fa-thumbs-up',
}, {
  id: 'HonorSystem',
  label: 'Honor System',
  count: 0,
  icon: 'far fa-meh-rolling-eyes',
}, {
  id: 'Disabled',
  label: 'Disabled',
  count: 0,
  icon: 'far fa-times-circle',
}]);

onMounted(() => {
cards.value = cards.value.map((c) => {
  const found = props.selfReportStats.find((s) => c.id === s.value);
  if (found) {
    // eslint-disable-next-line
    c.count = found.count;
  }
  return c;
});
});
</script>

<template>
  <div class="flex flex-column md:flex-row gap-2">
    <div class="flex flex-1" v-for="(card, index) in cards" :key="card.label">
      <Card class="h-full w-full" :pt="{ body: { class: 'p-3' }, content: { class: 'p-0' } }">
        <template #content>
          <div class="flex">
            <div class="flex-1 text-left">
              <div class="card-title uppercase text-muted mb-0 small">{{card.label}}</div>
              <span class="font-bold mb-0" :data-cy="`selfReportInfoCardCount_${card.id}`">{{ card.count }}</span> skills
            </div>
            <div class="">
              <i :class="`${card.icon} ${colors.getTextClass(index)}`" style="font-size: 2.2rem;" ></i>
            </div>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>
</style>