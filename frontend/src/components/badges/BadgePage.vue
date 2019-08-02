<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptions">
      <span slot="right-of-header">
        <i v-if="badge.endDate" class="fas fa-gem ml-2" style="font-size: 1.6rem; color: purple;"></i>
      </span>
    </page-header>


    <navigation :nav-items="[
          {name: 'Skills', iconClass: 'fa-graduation-cap', page: 'BadgeSkills'},
          {name: 'Users', iconClass: 'fa-users', page: 'BadgeUsers'},
          {name: 'Metrics', iconClass: 'fa-chart-bar', page: 'BadgeMetrics'},
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
        if (this.$route.params.badge) {
          this.badge = this.$route.params.badge;
          this.headerOptions = this.buildHeaderOptions(this.badge);
          this.isLoading = false;
        } else {
          BadgesService.getBadge(this.projectId, this.badgeId)
            .then((response) => {
              this.badge = response;
              this.headerOptions = this.buildHeaderOptions(this.badge);
              this.isLoading = false;
            });
        }
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
          }],
        };
      },
    },
  };
</script>

<style scoped>

</style>
