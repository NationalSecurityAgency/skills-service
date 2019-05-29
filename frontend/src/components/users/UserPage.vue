<template>
  <div>
    <page-header :loading="loading.userDetails" :options="headerOptons"/>

    <div v-if="userId" class="section">
      <navigation :nav-items="[
          {name: 'Client Display', iconClass: 'fa-user'},
          {name: 'Stats', iconClass: 'fa-chart-bar'},
          {name: 'Performed Skills', iconClass: 'fa-award'},
        ]">
        <template slot="Client Display">
          <section v-if="authToken && !loading.userDetails && !loading.userToken && !loading.availableVersions">
            <sub-page-header title="Client Display">
              <b-form class="float-right" inline>
                <label class="pr-3 d-none d-sm-inline font-weight-bold" for="version-select">Version: </label>
                <b-form-select
                  id="version-select"
                  class="version-select"
                  v-model="selectedVersion"
                  :options="versionOptions" />
                <inline-help
                  class="pl-2"
                  msg="Multiple skills versions can be defined if you have multiple versions of your application deployed." />
              </b-form>
            </sub-page-header>
            <skills-display
              :authentication-url="authenticationUrl"
              :version="selectedVersion"
              :project-id="projectId"
              :service-url="serviceUrl"/>
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
    </div>
  </div>
</template>

<script>
  import { SkillsDisplay } from '@skills/skills-client-vue/src/index';
  import { SECTION } from '../stats/SectionHelper';
  import Navigation from '../utils/Navigation';
  import SectionStats from '../stats/SectionStats';
  import UserSkillsPerformed from './UserSkillsPerformed';
  import UsersService from './UsersService';
  import PageHeader from '../utils/pages/PageHeader';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import InlineHelp from '../utils/InlineHelp';

  export default {
    name: 'UserPage',
    components: {
      PageHeader,
      Navigation,
      SectionStats,
      UserSkillsPerformed,
      SkillsDisplay,
      SubPageHeader,
      InlineHelp,
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
        loading: {
          userToken: true,
          availableVersions: true,
          userDetails: true,
        },
        section: SECTION.USERS,
        headerOptons: {},
        selectedVersion: 0,
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
        })
        .finally(() => {
          this.loading.userToken = false;
        });
      UsersService.getAvailableVersions(this.projectId)
        .then((result) => {
          this.versionOptions = result;
          this.selectedVersion = Math.max(...this.versionOptions);
        })
        .finally(() => {
          this.loading.availableVersions = false;
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
        this.loading.userDetails = true;
        UsersService.getUserUniqueSkillsCount(this.projectId, this.userId)
          .then((response) => {
            this.uniqueSkills = response;
            this.headerOptons = this.buildHeaderOptions();
            this.loading.userDetails = false;
          });
      },
      buildHeaderOptions() {
        return {
          icon: 'fas fa-user',
          title: `USER: ${this.userId}`,
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
  .version-select {
    width: 7rem;
  }
</style>
