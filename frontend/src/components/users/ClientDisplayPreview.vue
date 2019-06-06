<template>
  <div v-if="!loading.userToken && !loading.availableVersions">
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
      :authenticator="authenticator"
      :version="selectedVersion"
      :project-id="projectId"
      :service-url="serviceUrl"/>
  </div>

</template>

<script>
  import { SkillsDisplay } from '@skills/skills-client-vue/src/index';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import UsersService from './UsersService';
  import InlineHelp from '../utils/InlineHelp';

  export default {
    name: 'ClientDisplayPreview',
    components: {
      InlineHelp,
      SubPageHeader,
      SkillsDisplay,
    },
    data() {
      return {
        projectId: '',
        userId: '',
        authToken: '',
        loading: {
          userToken: true,
          availableVersions: true,
        },
        selectedVersion: 0,
        versionOptions: [],
      };
    },
    created() {
      this.projectId = this.$route.params.projectId;
      this.userId = this.$route.params.userId;
      this.totalPoints = this.$route.params.totalPoints;

      if (!this.$store.getters.isPkiAuthenticated) {
        UsersService.getUserToken(this.projectId, this.userId)
          .then((result) => {
            this.authToken = result;
          })
          .finally(() => {
            this.loading.userToken = false;
          });
      } else {
        this.loading.userToken = false;
      }
      UsersService.getAvailableVersions(this.projectId)
        .then((result) => {
          this.versionOptions = result;
          this.selectedVersion = Math.max(...this.versionOptions);
        })
        .finally(() => {
          this.loading.availableVersions = false;
        });
    },
    computed: {
      serviceUrl() {
        return window.location.origin;
      },
      authenticator() {
        if (this.$store.getters.isPkiAuthenticated) {
          return 'pki';
        }
        return `${this.serviceUrl}/admin/projects/${encodeURIComponent(this.projectId)}/token/${encodeURIComponent(this.userId)}`;
      },
    },
  };
</script>

<style scoped>

</style>
