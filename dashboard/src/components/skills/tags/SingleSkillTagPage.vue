/*
Copyright 2026 SkillTree

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
import ProjectPageHeader from "@/components/utils/pages/ProjectPageHeader.vue";
import {computed, onMounted} from "vue";
import {useSingleSkillTagState} from "@/stores/UseSingleSkillTagState.js";
import {useRoute} from "vue-router";
import Navigation from "@/components/utils/Navigation.vue";

const skillTagState = useSingleSkillTagState()
const route = useRoute()
const navItems = computed(() => {
  return [
    { name: 'Tagged Skills', iconClass: 'fa-graduation-cap', page: 'SkillTagSkills' },
    { name: 'Users', iconClass: 'fa-users', page: 'SkillTagUsers' },
  ];
})

onMounted(() => {
  skillTagState.loadSkillTagInfo(route.params.projectId, route.params.tagId)
})

const isLoading = computed(() => skillTagState.loadingSkillTag)
const headerOptions = computed(() => {
  const iconClass = 'fa-solid fa-tags'

  return {
    icon: `${iconClass}`,
    title: `TAG: ${skillTagState.skillTag?.tagValue || 'N/A' }`,
    stats: [{
      label: 'Tagged Skills',
      count: skillTagState.skillTag?.skills?.length || 0,
      icon: 'fa-solid fa-graduation-cap'
    }]
  }
})

</script>

<template>
  <div>
    <project-page-header :loading="isLoading" :options="headerOptions">
    </project-page-header>

    <navigation :nav-items="navItems" />
  </div>
</template>

<style scoped>

</style>