<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptions"/>

    <navigation v-if="userId" :nav-items="[
          {name: 'Client Display', iconClass: 'fa-user', page: 'ClientDisplayPreview'},
          {name: 'Performed Skills', iconClass: 'fa-award', page: 'UserSkillEvents'},
          {name: 'Metrics', iconClass: 'fa-chart-bar', page: 'UserMetrics'},
        ]">
    </navigation>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import Navigation from '../utils/Navigation';
  import PageHeader from '../utils/pages/PageHeader';

  const { mapActions, mapGetters } = createNamespacedHelpers('users');

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
      };
    },
    created() {
      this.projectId = this.$route.params.projectId;
      this.userId = this.$route.params.userId;
      this.loadUserDetails();
    },
    computed: {
      ...mapGetters([
        'numSkills',
        'userTotalPoints',
      ]),
      headerOptions() {
        return {
          icon: 'fas fa-user',
          title: `USER: ${this.userId}`,
          subTitle: `ID: ${this.userId}`,
          stats: [{
            label: 'Skills',
            count: this.numSkills,
          }, {
            label: 'Points',
            count: this.userTotalPoints,
          }],
        };
      },
    },
    methods: {
      ...mapActions([
        'loadUserDetailsState',
      ]),
      loadUserDetails() {
        this.isLoading = true;
        this.loadUserDetailsState({ projectId: this.projectId, userId: this.userId })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
  };
</script>

<style scoped>
  .version-select {
    width: 7rem;
  }
</style>
