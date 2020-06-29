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
          {name: 'Subjects', iconClass: 'fa-cubes', page: 'Subjects'},
          {name: 'Badges', iconClass: 'fa-award', page: 'Badges'},
          {name: 'Dependencies', iconClass: 'fa-vector-square', page: 'FullDependencyGraph'},
          {name: 'Cross Projects', iconClass: 'fa-handshake', page: 'CrossProjectsSkills'},
          {name: 'Levels', iconClass: 'fa-trophy', page: 'ProjectLevels'},
          {name: 'Users', iconClass: 'fa-users', page: 'ProjectUsers'},
          {name: 'Metrics', iconClass: 'fa-chart-bar', page: 'ProjectMetrics'},
          {name: 'Access', iconClass: 'fa-shield-alt', page: 'ProjectAccess'},
          {name: 'Settings', iconClass: 'fa-cogs', page: 'ProjectSettings'}
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
          icon: 'fas fa-list-alt',
          title: `PROJECT: ${this.project.name}`,
          subTitle: `ID: ${this.project.projectId}`,
          stats: [{
            label: 'Subjects',
            count: this.project.numSubjects,
          }, {
            label: 'Skills',
            count: this.project.numSkills,
          }, {
            label: 'Points',
            count: this.project.totalPoints,
            warnMsg: this.project.totalPoints < this.minimumPoints ? 'Project has insufficient points assigned. Skills cannot be achieved until project has at least 100 points.' : null,
          }, {
            label: 'Badges',
            count: this.project.numBadges,
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
