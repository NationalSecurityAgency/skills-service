/*
Copyright 2025 SkillTree

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
import {ref} from "vue";
import SkillsService from "@/components/skills/SkillsService.js";
import {useRoute} from "vue-router";
import AutoComplete from 'primevue/autocomplete'
import SkillsSelectorSlotProps from "@/components/skills/SkillsSelectorSlotProps.vue";

const route = useRoute()
const value = ref(null);
const items = ref([]);

const props = defineProps({
  name: {
    type: String,
    required: true,
  },
  label: {
    type: String,
    required: false,
  },
})

const search = (event) => {
  const projectId = route.params.projectId
  SkillsService.getProjectSkills(projectId, event.query).then((res) => {
    items.value = res;
  })
}

</script>

<template>
  <div class="flex flex-col gap-2">
    <label v-if="label" :for="name">{{ label }}</label>
    <AutoComplete
        :inputId="name"
        v-model="value"
        :suggestions="items"
        @complete="search"
        optionLabel="name"
        dropdown
        fluid>
      <template #option="slotProps">
        <skills-selector-slot-props :options="slotProps.option"/>
      </template>
    </AutoComplete>
  </div>
</template>

<style scoped>

</style>