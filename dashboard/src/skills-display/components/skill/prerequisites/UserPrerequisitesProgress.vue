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
import { onMounted, ref } from 'vue'
import VerticalProgressBar from '@/skills-display/components/progress/VerticalProgressBar.vue'

const props = defineProps({
  dependencies: {
    type: Array,
    required: true
  }
})

const numDependencies = ref(0)
const percentComplete = ref(0)
onMounted(() => {
  numDependencies.value = 0;
  let numCompleted = 0;
  const alreadyCountedIds = [];
  props.dependencies.forEach((dependency) => {
    const { dependsOn } = dependency;
    if (dependsOn) {
      const lookup = `${dependsOn.projectId}-${dependsOn.skillId}`;
      if (!alreadyCountedIds.includes(lookup)) {
        numDependencies.value += 1;
        if (dependency.achieved) {
          numCompleted += 1;
        }
        alreadyCountedIds.push(lookup);
      }
    }
  });
  if (numDependencies.value > 0 && numCompleted > 0) {
    percentComplete.value = Math.floor((numCompleted / numDependencies.value) * 100);
  }
})
</script>

<template>
  <div class="" data-cy="depsProgress">
    <div class="flex text-sm w-min-14rem items-center pb-1">
      <div class="flex-1">
        <Tag data-cy="numDeps" severity="info">{{ numDependencies }}</Tag>
        Prerequisites
      </div>
      <div data-cy="depsPercentComplete">
        {{ percentComplete }}%
      </div>
    </div>
    <vertical-progress-bar :total-progress="percentComplete" :bar-size="5" />
  </div>
</template>

<style scoped>

</style>