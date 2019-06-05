<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptons"/>

    <navigation v-if="userId" :nav-items="[
          {name: 'Client Display', iconClass: 'fa-user', page: 'ClientDisplayPreview'},
          {name: 'Performed Skills', iconClass: 'fa-award', page: 'UserSkillEvents'},
          {name: 'Stats', iconClass: 'fa-chart-bar', page: 'UserStats'},
        ]">
    </navigation>
  </div>
</template>

<script>
  import Navigation from '../utils/Navigation';
  import UsersService from './UsersService';
  import PageHeader from '../utils/pages/PageHeader';

  export default {
    name: 'UserPage',
    components: {
      PageHeader,
      Navigation,
    },
    data() {
      return {
        projectId: '',
        userId: '',
        isLoading: true,
        headerOptons: {},
      };
    },
    created() {
      this.projectId = this.$route.params.projectId;
      this.userId = this.$route.params.userId;
      this.loadUserDetails();
    },
    methods: {
      loadUserDetails() {
        this.isLoading = true;
        UsersService.getUserSkillsMetrics(this.projectId, this.userId)
          .then((response) => {
            this.headerOptons = this.buildHeaderOptions(response);
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      buildHeaderOptions(metrics) {
        return {
          icon: 'fas fa-user',
          title: `USER: ${this.userId}`,
          subTitle: `ID: ${this.userId}`,
          stats: [{
            label: 'Skills',
            count: metrics.numSkills,
          }, {
            label: 'Points',
            count: metrics.userTotalPoints,
          }],
        };
      },
    },
  };
</script>

<style scoped>
  .version-select {
    width: 7rem;
  }
</style>
