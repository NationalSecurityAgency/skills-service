<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptons"/>

    <section class="section" v-if="userId">
      <navigation :nav-items="[
          {name: 'Client Display', iconClass: 'fa-user'},
          {name: 'Stats', iconClass: 'fa-chart-bar'},
          {name: 'Performed Skills', iconClass: 'fa-award'},
        ]">
        <template slot="Client Display">
          <section v-if="authToken" class="">
            <sub-page-header title="Client Display">
              <b-form inline>
                <label class="pr-3 font-weight-bold" for="version-select">View for Version: </label>
                <b-form-select
                  id="version-select"
                  style="width: 10rem;"t stash
                  v-model="selectedVersion"
                  :options="versionOptions" />
              </b-form>
            </sub-page-header>
            <client-display-frame
              :authentication-url="authenticationUrl"
              service-url="http://localhost:8082"
              :version="selectedVersion"
              :auth-token="authToken"
              :project-id="projectId"/>
          </section>
        </template>
        <template slot="Stats">
          <section-stats :project-id="this.projectId" :section="section" :section-id-param="this.userId"
                         :num-days-to-show="365"></section-stats>
        </template>
        <template slot="Performed Skills">
          <user-skills-performed ref="skillsPerformedTable" :projectId="this.projectId" :userId="this.userId"/>
        </template>
      </navigation>
    </section>
  </div>
</template>

<script>
  import Navigation from '../utils/Navigation';
  import SectionStats from '../stats/SectionStats';
  import UserSkillsPerformed from './UserSkillsPerformed';
  import UsersService from './UsersService';
  import ClientDisplayFrame from './ClientDisplayFrame';
  import { SECTION } from '../stats/SectionHelper';
  import PageHeader from '../utils/pages/PageHeader';
  import SubPageHeader from '../utils/pages/SubPageHeader';

  export default {
    name: 'UserPage',
    components: {
      PageHeader,
      Navigation,
      SectionStats,
      UserSkillsPerformed,
      ClientDisplayFrame,
      SubPageHeader,
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
        section: SECTION.USERS,
        headerOptons: {},
        selectedVersion: null,
        versionOptions: [],
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
      UsersService.getAvailableVersions(this.projectId)
        .then((result) => {
          this.versionOptions = result;
          this.selectedVersion = Math.max(...this.versionOptions);
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
            this.headerOptons = this.buildHeaderOptions();
            this.isLoading = false;
          });
      },
      buildHeaderOptions() {
        return {
          icon: 'fas fa-user',
          title: `SUBJECT: ${this.userId}`,
          subTitle: `ID: ${this.userId}`,
          stats: [{
            label: 'Skills',
            count: this.uniqueSkills,
          }, {
            label: 'Points',
            count: this.totalPoints,
          }],
        };
      },
    },
  };
</script>

<style scoped>

</style>
