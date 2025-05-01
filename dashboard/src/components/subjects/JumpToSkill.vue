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
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { SkillsReporter } from '@skilltree/skills-client-js';
import SkillsService from '@/components/skills/SkillsService';
import SkillsSelector from '@/components/skills/SkillsSelector.vue';

const router = useRouter();
const route = useRoute();

let loading = ref(false);
let availableSkills = ref([]);
let search = ref('');

const searchChanged = (query) => {
    search.value = query;
    if (search.value && search.value.length > 0) {
      loading.value = true;
      SkillsService.getProjectSkills(route.params.projectId, search.value).then((result) => {
        availableSkills.value = result;
        loading.value = false;
      });
    } else {
      availableSkills.value = [];
    }
};

const navToSkill = (selectedItem) => {
  if (selectedItem) {
    router.push({
      name: 'SkillOverview',
      params: { projectId: selectedItem.projectId, subjectId: selectedItem.subjectId, skillId: selectedItem.skillId },
    });
    SkillsReporter.reportSkill('SearchandNavigatedirectlytoaskill');
  }
};
</script>

<template>
  <div class="flex mb-2 st-jump-to-skill-height">
    <div style="width: 100%;">
      <skills-selector :options="availableSkills"
                        class="search-and-nav border rounded-sm"
                        v-on:search-change="searchChanged"
                        v-on:added="navToSkill"
                        :is-loading="loading"
                        :show-dropdown="false"
                        placeholder="Search and Navigate directly to a skill"
                        aria-label="Search and Navigate directly to a skill"
                        placeholder-icon="fas fa-search"
                        select-label="Click to navigate"
                        :internal-search="false"
                        :empty-without-search="true"/>
    </div>
  </div>
</template>

<style scoped>
.search-and-nav {
  border-color: #d9d9d9 !important;
}

.st-jump-to-skill-height {
  height: 3rem;
}
@media only screen and (max-width: 400px) {
  .st-jump-to-skill-height {
    height: 4rem;
  }
}
</style>
