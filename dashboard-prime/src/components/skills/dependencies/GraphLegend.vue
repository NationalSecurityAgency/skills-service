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
defineProps(['items']);
</script>


<template>
  <div class="graph-legend" data-cy="graphLegend">
    <span class="font-italic">Legend:</span>
    <span v-for="item in items" v-bind:key="item.label" class="legend-list-item mb-0 ml-3">
      <span class="border rounded p-2">
        <i v-if="item.iconClass" :class="`fas ${item.iconClass}`" v-bind:style="{color: item.color}"></i>
      </span>
      <span class="ml-1">{{ item.label }}</span>
    </span>
  </div>
</template>


<style scoped>
.graph-legend {
  font-size: 1rem;
}
.legend-list-item i {
  font-size: 1.2rem;
}
</style>
