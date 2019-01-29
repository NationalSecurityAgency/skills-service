<template>
  <div>
    <section class="section skills-underline-container-1">
      <loading-container v-bind:is-loading="isLoading">

        <div class="columns has-text-left">
          <div class="column">
            <div class="subject-title ">
              <h1 class="title"><i class="fas fa-award"/> BADGE: {{ badge.name }}</h1>
              <h2 class="subtitle is-6 has-text-grey">ID: {{ badge.badgeId }}</h2>
            </div>
          </div>
          <div class="column">
            <div class="columns has-text-centered">
              <div class="column is-one-quarter">
                <div>
                  <p class="heading">Skills</p>
                  <p class="title">{{ badge.numSkills | number}}</p>
                </div>
              </div>

              <div class="column is-one-quarter">
                <div>
                  <p class="heading">Num Disabled Skills</p>
                  <p class="title">0</p>
                </div>
              </div>

              <div class="column is-one-quarter">
                <div>
                  <p class="heading">Total Points</p>
                  <p class="title">{{ badge.totalPoints | number }}</p>
                </div>
              </div>

              <div class="column is-one-quarter">
                <div>
                  <p class="heading">Users</p>
                  <p class="title">{{ badge.numUsers | number}}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </loading-container>
    </section>

    <hr class="skills-no-margin"/>

    <section class="section">
      <navigation :nav-items="[
          {name: 'Skills', iconClass: 'fa-graduation-cap'},
          {name: 'Users', iconClass: 'fa-users'},
          {name: 'Stats', iconClass: 'fa-chart-bar'},
        ]">
        <template slot="Skills">
          <!--<skills :project-id="this.$route.query.projectId" :subject-id="this.$route.query.subjectId" v-on:skills-change="loadBadge"/>-->
          <span>TODO: Skills Display</span>
        </template>
        <template slot="Users">
          <users :project-id="projectId" :badge-id="this.badgeId" />
        </template>
        <template slot="Stats">
          <project-stats :project-id="this.projectId"></project-stats>
        </template>
      </navigation>
    </section>
  </div>
</template>

<script>
  import BadgesService from './BadgesService';
  import Navigation from '../utils/Navigation';
  // import Skills from '../skills/Skills';
  import LoadingContainer from '../utils/LoadingContainer';
  import ProjectStats from '../stats/ProjectStats';
  import Users from '../users/Users';

  export default {
    name: 'BadgePage',
    components: { ProjectStats, LoadingContainer, Navigation, Users },
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
        serverErrors: [],
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
    methods: {
      loadBadge() {
        this.isLoading = false;
        BadgesService.getBadge(this.projectId, this.badgeId)
          .then((response) => {
            this.badge = response;
            this.isLoading = false;
          })
          .catch((e) => {
            this.serverErrors.push(e);
            this.isLoading = false;
            throw e;
        });
      },
    },
  };
</script>

<style scoped>

</style>
