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
import { computed } from 'vue';
import ProjectDates from '@/components/projects/ProjectDates.vue';
import UserRolesUtil from '@/components/utils/UserRolesUtil';
import Badge from 'primevue/badge';
import { useAdminProjectsState } from '@/stores/UseAdminProjectsState.js'

const props = defineProps(['project']);
const project = props.project;

const projectsState = useAdminProjectsState()

const hasIssues = computed(() => {
  return project.numErrors && project.numErrors > 0;
});
const numIssues = computed(() => {
  return project.numErrors;
});
const isReadOnlyProj = computed(() => {
  return UserRolesUtil.isReadOnlyProjRole(project.userRole);
});

const userRoleForDisplay = computed(() => {
  return UserRolesUtil.userRoleFormatter(project.userRole);
});

const numIssuesForDisplay = computed(() => {
  return numIssues;
});
</script>

<template>
  <div class="text-right">
    <div class="flex"
         :class="{
            'flex-col gap-2 justify-center items-center': projectsState.shouldTileProjectsCards,
            '': !projectsState.shouldTileProjectsCards
          }">
      <div class="flex-1 text-left small" data-cy="ProjectCardFooter_issues">
        <i class="fas fa-user-shield text-purple-500" style="font-size: 1.05rem;" aria-hidden="true"></i> <i>Role:</i> <span data-cy="userRole">{{ userRoleForDisplay }}</span>
        <span v-if="!isReadOnlyProj" class="ml-2">
          <span v-if="!hasIssues"><i class="fas fa-check-circle text-green-500" style="font-size: 1rem;"
                                     aria-hidden="true"></i> <span data-cy="noIssues">No Issues</span></span>
          <span v-if="hasIssues"><i class="fas fa-exclamation-triangle text-red-500" style="font-size: 1rem;"
                                    aria-hidden="true"></i>
            There {{ numIssues > 1 ? 'are' : 'is' }} <span style="font-size: 1rem;"><Badge variant="danger">{{ numIssuesForDisplay }}</Badge></span> {{ numIssues > 1 ? 'issues' : 'issue' }} to address </span>
        </span>
      </div>
      <div :class="{
            'text-left': projectsState.shouldTileProjectsCards,
            'text-right': !projectsState.shouldTileProjectsCards
          }" data-cy="projectCreated">
        <ProjectDates :created="project.created"/>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
