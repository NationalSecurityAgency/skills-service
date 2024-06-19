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
import { ref, watch } from 'vue';
import SelectButton from 'primevue/selectbutton';

const emit = defineEmits(['value-changed']);

const msg = ref({
  directed: 'Directed adheres to the to and from data of the edges. A --> B so B is a level lower than A.',
  hubsize: 'Hubsize takes the nodes with the most edges and puts them at the top. From that the rest of the hierarchy is evaluated.',
})
// optionLabel="value" dataKey="value"
const options = [{
  value: 'directed',
  label: 'Directed',
  icon: 'fas fa-vector-square'
}, {
  value: 'hubsize',
  label: 'Hubsize',
  icon: 'fas fa-bezier-curve'
}];
const sortMethod = ref(options[0]);

const notifyOfChange = () => {
  emit('value-changed', sortMethod.value.value);
};

watch(sortMethod, notifyOfChange);
</script>

<template>
  <div class="deps-overlay">
    <SelectButton v-model="sortMethod" :options="options" optionLabel="value" dataKey="value">
      <template #option="slotProps">
        <i :class="slotProps.option.icon"></i> {{ slotProps.option.label }}
      </template>
    </SelectButton>
  </div>
</template>

<style>
.deps-overlay {
  z-index: 99;
  position: relative;
}

</style>
<style scoped>
</style>
