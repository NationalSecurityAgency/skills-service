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
  import ProjectService from './ProjectService';
  import Navigation from '../utils/Navigation';
  import PageHeader from '../utils/pages/PageHeader';

  export default {
    name: 'ProjectPage',
    components: {
      PageHeader,
      Navigation,
    },
    data() {
      return {
        isLoading: true,
        headerOptions: {},
      };
    },
    mounted() {
      this.loadProjects();
    },
    computed: {
      minimumPoints() {
        return this.$store.state.minimumProjectPoints;
      },
    },
    methods: {
      loadProjects() {
        this.isLoading = true;
        if (this.$route.params.project) {
          this.headerOptions = this.buildHeaderOptions(this.$route.params.project);
          this.isLoading = false;
        } else {
          ProjectService.getProjectDetails(this.$route.params.projectId)
            .then((response) => {
              this.headerOptions = this.buildHeaderOptions(response);
            })
            .finally(() => {
              this.isLoading = false;
            });
        }
      },
      buildHeaderOptions(project) {
        return {
          icon: 'fas fa-list-alt',
          title: `PROJECT: ${project.name}`,
          subTitle: `ID: ${project.projectId}`,
          stats: [{
            label: 'Subjects',
            count: project.numSubjects,
          }, {
            label: 'Skills',
            count: project.numSkills,
          }, {
            label: 'Points',
            count: project.totalPoints,
            warnMsg: project.totalPoints < this.minimumPoints ? 'Project has insufficient points assigned. Skills cannot be achieved until project has at least 100 points.' : null,
          }, {
            label: 'Badges',
            count: project.numBadges,
          }],
        };
      },
    },
  };
</script>

<style scoped>

</style>
