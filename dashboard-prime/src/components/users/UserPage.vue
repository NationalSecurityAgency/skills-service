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
import { ref, computed, onMounted } from 'vue'
import PageHeader from '@/components/utils/pages/PageHeader.vue'
import Navigation from '@/components/utils/Navigation.vue'
import { useRoute } from 'vue-router'
import { useProjectUserState } from '@/stores/UseProjectUserState.js'
import UsersService from '@/components/users/UsersService.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

const route = useRoute()
const projectUserState = useProjectUserState()
const appConfig = useAppConfig()

const isLoading = ref(true)
const userTitle = ref( '')
const userIdForDisplay = ref( '')
const tags = ref( '')

const headerOptions = computed(() => {
  return {
    icon: 'fas fa-user skills-color-users',
    title: `USER: ${userTitle.value}`,
    subTitle: `ID: ${userIdForDisplay.value}`,
    stats: [{
      label: 'Skills',
      count: projectUserState.numSkills,
      icon: 'fas fa-graduation-cap skills-color-skills',
    }, {
      label: 'Points',
      count: projectUserState.userTotalPoints,
      icon: 'far fa-arrow-alt-circle-up skills-color-points',
    }],
  }
})

const navItems = computed(() => {
  return [
    { name: 'Client Display', iconClass: 'fa-user skills-color-skills', page: 'SkillsDisplaySkillsDisplayPreviewProject' },
    { name: 'Performed Skills', iconClass: 'fa-award skills-color-events', page: 'UserSkillEvents' },
  ];
})

onMounted(() => {
  projectUserState.loadUserDetailsState(route.params.projectId, route.params.userId)
  loadUserInfo()
})


const loadUserInfo = () => {
  userTitle.value = route.params.userId;
  userIdForDisplay.value =route.params.userId;
  let userTags = Promise.resolve();

  if (appConfig.userPageTagsToDisplay) {
    userTags = UsersService.getUserTags(route.params.userId).then((response) => {
      tags.value = processUserTags(response);
    });
  }

  const userDetails = UsersService.getUserInfo(route.params.projectId, route.params.userId)
    .then((result) => {
      userIdForDisplay.value = result.userIdForDisplay
      userTitle.value = result.first && result.last ? `${result.first} ${result.last}` : result.userIdForDisplay
      return projectUserState.loadUserDetailsState(route.params.projectId, route.params.userId)
    })


  Promise.all([userTags, userDetails]).finally(() => {
    isLoading.value = false;
  });
}

const processUserTags = (userTags) =>{
  const userPageTags = appConfig.userPageTagsToDisplay;
  const tags = [];
  if (userPageTags) {
    const tagSections = userPageTags.split('|');
    tagSections.forEach((section) => {
      const [key, label] = section.split('/');
      tags.push({
        key, label,
      });
    });
  }

  const processedTags = [];
  tags.forEach((tag) => {
    const userTag = userTags.filter((ut) => ut.key === tag.key);
    if (userTag) {
      const values = userTag.map((ut) => ut.value);
      if (values.length > 0) {
        processedTags.push({ label: tag.label, key: tag.key, value: values });
      }
    }
  });
  return processedTags;
}

</script>

<template>
<div>
  <page-header :loading="isLoading" :options="headerOptions">
  </page-header>

  <navigation v-if="!isLoading" :nav-items="navItems">
  </navigation>
</div>
</template>

<style scoped>

</style>