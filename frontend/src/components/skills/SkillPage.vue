<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptions"/>

    <navigation :nav-items="navItems">
    </navigation>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import SkillsService from './SkillsService';
  import Navigation from '../utils/Navigation';
  import PageHeader from '../utils/pages/PageHeader';

  const { mapGetters } = createNamespacedHelpers('subjects');

  export default {
    name: 'SkillPage',
    components: {
      PageHeader,
      Navigation,
    },
    data() {
      return {
        isLoading: true,
        skill: {},
        subjectId: '',
        headerOptions: {},
      };
    },
    mounted() {
      this.loadSkill();
    },
    computed: {
      ...mapGetters([
        'subject',
      ]),
      navItems() {
        const items = [];
        items.push({ name: 'Overview', iconClass: 'fa-info-circle', page: 'SkillOverview' });
        items.push({ name: 'Dependencies', iconClass: 'fa-vector-square', page: 'SkillDependencies' });
        items.push({ name: 'Users', iconClass: 'fa-users', page: 'SkillUsers' });
        const addEventDisabled = this.subject.totalPoints < this.$store.state.minimumSubjectPoints;
        const msg = addEventDisabled ? `Subject needs at least ${this.$store.state.minimumSubjectPoints} points before events can be added` : '';
        items.push({
          name: 'Add Event', iconClass: 'fa-user-plus', page: 'AddSkillEvent', isDisabled: addEventDisabled, msg: msg,
        });
        items.push({ name: 'Metrics', iconClass: 'fa-chart-bar', page: 'SkillMetrics' });
        return items;
      },
    },
    watch: {
      // Vue caches components and when re-directed to the same component the path will be pushed
      // to the url but the component will NOT be re-mounted therefore we must listen for events and re-load
      // the data; alternatively could update
      //    <router-view :key="$route.fullPath"/>
      // but components will never get cached - caching maybe important for components that want to update
      // the url so the state can be re-build later (example include browsing a map or dependency graph in our case)
      '$route.params.skillId': function skillChange() {
        this.loadSkill();
      },
    },
    methods: {
      loadSkill() {
        this.isLoading = true;
        SkillsService.getSkillDetails(this.$route.params.projectId, this.$route.params.subjectId, this.$route.params.skillId)
          .then((response) => {
            this.skill = Object.assign(response, { subjectId: this.$route.params.subjectId });
            this.headerOptions = this.buildHeaderOptions(this.skill);
            this.isLoading = false;
          });
      },
      buildHeaderOptions(skill) {
        return {
          icon: 'fas fa-graduation-cap',
          title: `SKILL: ${skill.name}`,
          subTitle: `ID: ${skill.skillId}`,
          stats: [{
            label: 'Points',
            count: skill.totalPoints,
          }],
        };
      },
    },
  };
</script>

<style scoped>

</style>
