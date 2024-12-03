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
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute()
const isProjectLevel = computed(() => {
  return !(route.params.skillId || route.params.badgeId || route.params.subjectId || (route.params.tagKey && route.params.tagFilter))
})
const isUserArchiveRoute = computed(() => {
  return route.name === 'UserArchivePage';
});
</script>

<template>
  <div>
    <sub-page-header title="Users">
      <div v-if="isProjectLevel">
        <router-link :to="{ name: 'UserArchivePage' }" v-if="!isUserArchiveRoute" tabindex="-1">
          <SkillsButton size="small" icon="fas fa-archive" label="User Archive" data-cy="userArchiveBtn" />
        </router-link>
        <router-link :to="{ name: 'ProjectUsers' }" v-if="isUserArchiveRoute" tabindex="-1">
          <SkillsButton id="backToProjectUsersBtn" size="small" icon="fas fa-arrow-alt-circle-left" label="Back" />
        </router-link>
      </div>
    </sub-page-header>
    <div style="width: 99%;">
      <router-view></router-view>
    </div>
  </div>
</template>

<style scoped></style>
