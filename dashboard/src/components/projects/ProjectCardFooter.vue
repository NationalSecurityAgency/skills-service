/*
Copyright 2020 SkillTree

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
<template>
  <div class="text-right">
    <div class="row">
      <div class="col text-left small" data-cy="ProjectCardFooter_issues">
        <i class="fas fa-user-shield text-success" style="font-size: 1.05rem;" aria-hidden="true"></i> <i>Role:</i> <span data-cy="userRole">{{ project.userRole | userRole }}</span>
        <span v-if="!isReadOnlyProj" class="ml-2">
          <span v-if="!hasIssues"><i class="fas fa-check-circle text-success" style="font-size: 1rem;"
                                     aria-hidden="true"></i> <span data-cy="noIssues">No Issues</span></span>
          <span v-if="hasIssues"><i class="fas fa-exclamation-triangle text-danger" style="font-size: 1rem;"
                                    aria-hidden="true"></i>
            There are <span style="font-size: 1rem;"><b-badge variant="danger">{{ numIssues | number }}</b-badge></span> issues to address </span>
        </span>
      </div>
      <div class="col text-right" data-cy="projectCreated">
        <project-dates :created="project.created"/>
      </div>
    </div>
  </div>
</template>

<script>
  import ProjectDates from '@/components/projects/ProjectDates';
  import UserRolesUtil from '@/components/utils/UserRolesUtil';

  export default {
    name: 'ProjectCardFooter',
    components: { ProjectDates },
    props: {
      project: Object,
    },
    computed: {
      hasIssues() {
        return this.project.numErrors && this.project.numErrors > 0;
      },
      numIssues() {
        return this.project.numErrors;
      },
      isReadOnlyProj() {
        return UserRolesUtil.isReadOnlyProjRole(this.project.userRole);
      },
    },
  };
</script>

<style scoped>

</style>
