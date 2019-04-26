<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptions"/>

    <section class="section">
      <navigation :nav-items="[
          {name: 'Skills', iconClass: 'fa-graduation-cap'},
          {name: 'Users', iconClass: 'fa-users'},
          {name: 'Stats', iconClass: 'fa-chart-bar'},
        ]">
        <template slot="Skills">
          <badge-skills :project-id="projectId" :badge-id="badgeId" v-on:skills-changed="loadBadge"></badge-skills>
        </template>
        <template slot="Users">
          <users :project-id="projectId" :badge-id="this.badgeId" />
        </template>
        <template slot="Stats">
          <section-stats :project-id="this.projectId" :section="section" :section-id-param="this.badgeId"></section-stats>
        </template>
      </navigation>
    </section>
  </div>
</template>

<script>
  import BadgesService from './BadgesService';
  import Navigation from '../utils/Navigation';
  import SectionStats from '../stats/SectionStats';
  import Users from '../users/Users';
  import BadgeSkills from './BadgeSkills';
  import { SECTION } from '../stats/SectionHelper';
  import PageHeader from '../utils/pages/PageHeader';

  export default {
    name: 'BadgePage',
    components: {
      PageHeader,
      BadgeSkills,
      SectionStats,
      Navigation,
      Users,
    },
    breadcrumb() {
      return {
        label: `BADGE: ${this.badgeId}`,
        parentsList: [
          {
            to: {
              name: 'ProjectPage',
              params: {
                projectId: this.projectId,
              },
            },
            label: `PROJECT: ${this.projectId}`,
          },
          {
            to: {
              name: 'HomePage',
            },
            label: 'Home',
          },
        ],
      };
    },
    data() {
      return {
        isLoading: true,
        badge: {},
        projectId: '',
        badgeId: '',
        section: SECTION.BADGES,
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
          title: `SUBJECT: ${badge.name}`,
          subTitle: `ID: ${badge.skillId}`,
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
