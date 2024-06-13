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
            'flex-column gap-2 justify-content-left': projectsState.shouldTileProjectsCards,
            '': !projectsState.shouldTileProjectsCards
          }">
      <div class="flex-1 text-left small" data-cy="ProjectCardFooter_issues">
        <i class="fas fa-user-shield text-success" style="font-size: 1.05rem;" aria-hidden="true"></i> <i>Role:</i> <span data-cy="userRole">{{ userRoleForDisplay }}</span>
        <span v-if="!isReadOnlyProj" class="ml-2">
          <span v-if="!hasIssues"><i class="fas fa-check-circle text-success" style="font-size: 1rem;"
                                     aria-hidden="true"></i> <span data-cy="noIssues">No Issues</span></span>
          <span v-if="hasIssues"><i class="fas fa-exclamation-triangle text-danger" style="font-size: 1rem;"
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
