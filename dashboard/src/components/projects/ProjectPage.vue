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
  <div>
    <page-header :loading="isLoading" :options="headerOptions"/>

    <navigation v-if="!isLoading" :nav-items="[
          {name: 'Subjects', iconClass: 'fa-cubes skills-color-subjects', page: 'Subjects'},
          {name: 'Badges', iconClass: 'fa-award skills-color-badges', page: 'Badges'},
          {name: 'Dependencies', iconClass: 'fa-vector-square skills-color-dependencies', page: 'FullDependencyGraph'},
          {name: 'Cross Projects', iconClass: 'fa-handshake skills-color-crossProjects', page: 'CrossProjectsSkills'},
          {name: 'Levels', iconClass: 'fa-trophy skills-color-levels', page: 'ProjectLevels'},
          {name: 'Users', iconClass: 'fa-users skills-color-users', page: 'ProjectUsers'},
          {name: 'Metrics', iconClass: 'fa-chart-bar skills-color-metrics', page: 'ProjectMetrics'},
          {name: 'Access', iconClass: 'fa-shield-alt skills-color-access', page: 'ProjectAccess'},
          {name: 'Settings', iconClass: 'fa-cogs skills-color-settings', page: 'ProjectSettings'}
        ]">
    </navigation>
  </div>

</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import Navigation from '../utils/Navigation';
  import PageHeader from '../utils/pages/PageHeader';

  const { mapActions, mapGetters, mapMutations } = createNamespacedHelpers('projects');

  export default {
    name: 'ProjectPage',
    components: {
      PageHeader,
      Navigation,
    },
    data() {
      return {
        isLoading: true,
      };
    },
    mounted() {
      this.loadProjects();
    },
    computed: {
      ...mapGetters([
        'project',
      ]),
      headerOptions() {
        if (!this.project) {
          return {};
        }
        return {
          icon: 'fas fa-list-alt skills-color-projects',
          title: `PROJECT: ${this.project.name}`,
          subTitle: `ID: ${this.project.projectId}`,
          stats: [{
            label: 'Subjects',
            count: this.project.numSubjects,
            icon: 'fas fa-cubes skills-color-subjects',
          }, {
            label: 'Skills',
            count: this.project.numSkills,
            icon: 'fas fa-graduation-cap skills-color-skills',
          }, {
            label: 'Points',
            count: this.project.totalPoints,
            warnMsg: this.project.totalPoints < this.minimumPoints ? 'Project has insufficient points assigned. Skills cannot be achieved until project has at least 100 points.' : null,
            icon: 'far fa-arrow-alt-circle-up skills-color-points',
          }, {
            label: 'Badges',
            count: this.project.numBadges,
            icon: 'fas fa-award skills-color-badges',
          }],
        };
      },
      minimumPoints() {
        return this.$store.getters.config.minimumProjectPoints;
      },
    },
    methods: {
      ...mapActions([
        'loadProjectDetailsState',
      ]),
      ...mapMutations([
        'setProject',
      ]),
      loadProjects() {
        this.isLoading = true;
        if (this.$route.params.project) {
          this.setProject(this.$route.params.project);
          this.isLoading = false;
        } else {
          this.loadProjectDetailsState({ projectId: this.$route.params.projectId })
            .finally(() => {
              this.isLoading = false;
            });
        }
      },
    },
  };
</script>

<style scoped>

</style>
