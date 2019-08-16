<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptions">
      <span slot="right-of-header">
        <i v-if="badge && badge.endDate" class="fas fa-gem ml-2" style="font-size: 1.6rem; color: purple;"></i>
      </span>
    </page-header>


    <navigation :nav-items="[
          {name: 'Skills', iconClass: 'fa-graduation-cap', page: 'GlobalBadgeSkills'},
          {name: 'Levels', iconClass: 'fa-trophy', page: 'GlobalBadgeLevels'},
          // {name: 'Users', iconClass: 'fa-users', page: 'GlobalBadgeUsers'},
          // {name: 'Metrics', iconClass: 'fa-chart-bar', page: 'GlobalBadgeMetrics'},
        ]">
    </navigation>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import Navigation from '../../utils/Navigation';
  import PageHeader from '../../utils/pages/PageHeader';

  const { mapActions, mapGetters, mapMutations } = createNamespacedHelpers('badges');

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
      ...mapGetters([
        'badge',
      ]),
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
      ...mapActions([
        'loadGlobalBadgeDetailsState',
      ]),
      ...mapMutations([
        'setBadge',
      ]),
      loadBadge() {
        this.isLoading = false;
        if (this.$route.params.badge) {
          this.setBadge(this.$route.params.badge);
          this.isLoading = false;
        } else {
          this.loadGlobalBadgeDetailsState({ badgeId: this.badgeId })
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
