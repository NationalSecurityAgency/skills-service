<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptions">
      <span slot="right-of-header">
        <i v-if="badge && badge.endDate" class="fas fa-gem ml-2" style="font-size: 1.6rem; color: purple;"></i>
      </span>
    </page-header>


    <navigation :nav-items="[
          {name: 'Skills', iconClass: 'fa-graduation-cap', page: 'GlobalBadgeSkills'},
          // {name: 'Levels', iconClass: 'fa-users', page: 'GlobalBadgeLevels'},
          // {name: 'Users', iconClass: 'fa-users', page: 'GlobalBadgeUsers'},
          // {name: 'Metrics', iconClass: 'fa-chart-bar', page: 'GlobalBadgeMetrics'},
        ]">
    </navigation>
  </div>
</template>

<script>
  import Navigation from '../../utils/Navigation';
  import PageHeader from '../../utils/pages/PageHeader';
  import GlobalBadgeService from './GlobalBadgeService';

  export default {
    name: 'BadgePage',
    components: {
      PageHeader,
      Navigation,
    },
    data() {
      return {
        isLoading: true,
        projectId: '',
        badgeId: '',
        badge: {},
      };
    },
    created() {
      this.projectId = this.$route.params.projectId;
      this.badgeId = this.$route.params.badgeId;
    },
    mounted() {
      this.loadBadge();
    },
    computed: {
      headerOptions() {
        if (!this.badge) {
          return {};
        }
        return {
          icon: 'fas fa-award',
          title: `BADGE: ${this.badge.name}`,
          subTitle: `ID: ${this.badge.badgeId}`,
          stats: [{
            label: 'Skills',
            count: this.badge.numSkills,
          }, {
            label: 'Points',
            count: this.badge.totalPoints,
          }],
        };
      },
    },
    methods: {
      loadBadge() {
        this.isLoading = false;
        if (this.$route.params.badge) {
          this.badge = this.$route.params.badge;
          this.isLoading = false;
        } else {
          GlobalBadgeService.getBadge(this.badgeId)
            .then((response) => {
              this.badge = response;
            }).finally(() => {
              this.isLoading = false;
            });
        }
      },
    },
  };
</script>

<style scoped>

</style>
