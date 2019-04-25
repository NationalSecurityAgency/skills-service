<template>
  <div>
    <section class="section">
      <loading-container v-bind:is-loading="isLoading">

        <div class="columns has-text-left">
          <div class="column">
            <div class="subject-title">
              <h1 class="title"><i class="fas fa-list-alt has-text-link"/>User: {{ userId }}</h1>
              <h2 class="subtitle is-6 has-text-grey">ID: {{ userId }}</h2>
            </div>
          </div>
          <div class="column">
            <div class="columns has-text-centered">
              <div class="column is-one-half">
                <div>
                  <p class="heading">Skills</p>
                  <p class="title">{{ uniqueSkills }}</p>
                </div>
              </div>
              <div class="column is-one-half">
                <div>
                  <p class="heading">Total Points</p>
                  <p class="title">{{ totalPoints}}</p>
                </div>
              </div>
            </div>

          </div>
        </div>

      </loading-container>
    </section>

    <hr class="skills-no-margin"/>

    <section class="section" v-if="userId">
      <navigation :nav-items="[
          {name: 'Client Display', iconClass: 'fa-cubes'},
          {name: 'Stats', iconClass: 'fa-chart-bar'},
          {name: 'Performed Skills', iconClass: 'fa-award'},
        ]">
        <template slot="Client Display">
          <section v-if="authToken" class="">
            <client-display-frame
              :authentication-url="authenticationUrl"
              :auth-token="authToken"
              :project-id="projectId" />
          </section>
        </template>
        <template slot="Stats">
          <project-stats :project-id="this.projectId"></project-stats>
        </template>
        <template slot="Performed Skills">
          <user-skills-performed ref="skillsPerformedTable" :projectId="this.projectId" :userId="this.userId" />
        </template>
      </navigation>
    </section>
  </div>
</template>

<script>
  import Navigation from '../utils/Navigation';
  import LoadingContainer from '../utils/LoadingContainer';
  import ProjectStats from '../stats/ProjectStats';
  import UserSkillsPerformed from './UserSkillsPerformed';
  import UsersService from './UsersService';
  import ClientDisplayFrame from './ClientDisplayFrame';

  export default {
    name: 'UserPage',
    components: {
      LoadingContainer,
      Navigation,
      ProjectStats,
      UserSkillsPerformed,
      ClientDisplayFrame,
    },
    breadcrumb() {
      return {
        label: `USER: ${this.userId}`,
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
        projectId: '',
        userId: '',
        authToken: '',
        authenticationUrl: '',
        totalPoints: 0,
        uniqueSkills: 0,
        isLoading: true,
      };
    },
    created() {
      this.projectId = this.$route.params.projectId;
      this.userId = this.$route.params.userId;
      this.totalPoints = this.$route.params.totalPoints;
      this.authenticationUrl = `${this.serviceUrl}/admin/projects/${this.projectId}/token/${this.userId}`;
      UsersService.getUserToken(this.projectId, this.userId)
        .then((result) => {
          this.authToken = result;
        });
      this.loadUserDetails();
    },
    computed: {
      serviceUrl() {
        return window.location.origin;
      },
    },
    methods: {
      loadUserDetails() {
        this.isLoading = true;
        UsersService.getUserUniqueSkillsCount(this.projectId, this.userId)
          .then((response) => {
            this.uniqueSkills = response;
            this.isLoading = false;
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
