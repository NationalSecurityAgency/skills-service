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
import { ref } from "vue";
import SkillsService from "@/components/skills/SkillsService.js";
import {useRoute} from "vue-router";
import AutoComplete from 'primevue/autocomplete'

const route = useRoute()
const value = ref(null);
const items = ref([]);

const search = (event) => {
  const projectId = route.params.projectId
  SkillsService.getProjectSkills(projectId, event.query).then((res) => {
    items.value = res;
  })
}

</script>

<template>
<div>
  <AutoComplete
      v-model="value"
      :suggestions="items"
      @complete="search" />
</div>
</template>

<style scoped>

</style>