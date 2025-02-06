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
import MetricNavCard from "@/components/metrics/projectNav/MetricNavCard.vue";
import {useColors} from "@/skills-display/components/utilities/UseColors.js";

defineProps(['navCards'])
const colors = useColors()

</script>

<template>
  <div class="flex flex-wrap gap-4 flex-col lg:flex-row mt-6">
      <metric-nav-card v-for="(navItem, index) in navCards" :key="navItem.title" class="flex-1"
                       :title="navItem.title" :subtitle="navItem.subtitle" :description="navItem.description"
                       :path-name="navItem.pathName" :icon="`${navItem.icon} ${colors.getTextClass(index)}`" />
  </div>
</template>

<style scoped>

</style>