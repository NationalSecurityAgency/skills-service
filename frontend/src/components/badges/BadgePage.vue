<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptions"/>

    <navigation :nav-items="[
          {name: 'Skills', iconClass: 'fa-graduation-cap', page: 'BadgeSkills'},
          {name: 'Users', iconClass: 'fa-users', page: 'BadgeUsers'},
          {name: 'Stats', iconClass: 'fa-chart-bar', page: 'BadgeStats'},
        ]">
    </navigation>
  </div>
</template>

<script>
  import BadgesService from './BadgesService';
  import Navigation from '../utils/Navigation';
  import PageHeader from '../utils/pages/PageHeader';

  export default {
    name: 'BadgePage',
    components: {
      PageHeader,
      Navigation,
    },
    data() {
      return {
        isLoading: true,
        badge: {},
        projectId: '',
        badgeId: '',
        headerOptions: {},
      };
    },
    created() {
      this.projectId = this.$route.params.projectId;
      this.badgeId = this.$route.params.badgeId;
    },
    mounted() {
      this.loadBadge();
    },
    methods: {
      loadBadge() {
        this.isLoading = false;
        BadgesService.getBadge(this.projectId, this.badgeId)
          .then((response) => {
            this.badge = response;
            this.headerOptions = this.buildHeaderOptions(this.badge);
            this.isLoading = false;
          });
      },
      buildHeaderOptions(badge) {
        return {
          icon: 'fas fa-award',
          title: `BADGE: ${badge.name}`,
          subTitle: `ID: ${badge.badgeId}`,
          stats: [{
            label: 'Skills',
            count: badge.numSkills,
          }, {
            label: 'Points',
            count: badge.totalPoints,
          }, {
            label: 'Users',
            count: badge.numUsers,
          }],
        };
      },
    },
  };
</script>

<style scoped>

</style>
