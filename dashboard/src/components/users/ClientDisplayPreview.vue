/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
  <div>
    <sub-page-header title="Client Display">
      <b-form class="float-right" inline>
        <label class="pr-3 d-none d-sm-inline font-weight-bold"
               aria-label="Select skill version"
               for="version-select">Version: </label>
        <b-form-select
          id="version-select"
          class="version-select"
          v-model="selectedVersion"
          :options="versionOptions"
          data-cy="clientDisplaySkillVersionSelect"
          @change="versionChanged"/>
        <inline-help
          class="pl-2"
          msg="Multiple skills versions can be defined if you have multiple versions of your application deployed." />
      </b-form>
    </sub-page-header>
    <loading-container :is-loading="checkingAccess">
      <div id="skills-client-container" ref="skillsDisplayRef" @route-changed="skillsDisplayRouteChanged"></div>
      <div v-if="!canAccess" class="container">
        <div class="row justify-content-center">
          <div class="col-md-6 mt-3">
            <div class="text-center mt-5">
              <div class="h2"><i class="fas fa-user-slash fa-2x" aria-hidden="true"/> Access Revoked</div>
              <div>This user's access was previously revoked, their Client Display is disabled until they are granted access. </div>
            </div>
          </div>
        </div>
      </div>
    </loading-container>
  </div>

</template>

<script>
  import { SkillsDisplayJS, SkillsReporter } from '@skilltree/skills-client-js';
  import LoadingContainer from '@/components/utils/LoadingContainer';
  import ProjConfigMixin from '@/components/projects/ProjConfigMixin';
  import SkillsDisplayOptionsMixin from '../myProgress/SkillsDisplayOptionsMixin';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import UsersService from './UsersService';
  import InlineHelp from '../utils/InlineHelp';

  export default {
    name: 'ClientDisplayPreview',
    mixins: [SkillsDisplayOptionsMixin, ProjConfigMixin],
    components: {
      InlineHelp,
      SubPageHeader,
      LoadingContainer,
    },
    data() {
      return {
        displayLoaded: false,
        clientDisplay: null,
        projectId: '',
        inviteOnly: false,
        userIdParam: '',
        canAccess: true,
        checkingAccess: true,
        loading: {
          userInfo: true,
          availableVersions: true,
        },
        selectedVersion: 0,
        versionOptions: [],
        theme: {
          disableSkillTreeBrand: true,
          disableBreadcrumb: true,
          maxWidth: '100%',
          pageTitleFontSize: '1.5rem',
          backButton: {
            padding: '5px 10px',
            fontSize: '12px',
            lineHeight: '1.5',
          },
        },
      };
    },
    created() {
      this.projectId = this.$route.params.projectId;
      if (this.$store.getters.isPkiAuthenticated) {
        // dn is provided when routed form other pages
        if (this.$route.params.dn && !this.$route.params.userId) {
          this.userIdParam = this.$route.params.dn;
          this.loading.userInfo = false;
        } else {
          this.userIdParam = {
            id: this.$route.params.userId,
            idType: 'ID',
          };
          this.loading.userInfo = false;
        }
      } else {
        this.userIdParam = this.$route.params.userId;
        this.loading.userInfo = false;
      }

      this.totalPoints = this.$route.params.totalPoints;

      UsersService.getAvailableVersions(this.projectId)
        .then((result) => {
          this.versionOptions = result;
          this.selectedVersion = Math.max(...this.versionOptions);
        })
        .finally(() => {
          this.loading.availableVersions = false;
        });
    },
    mounted() {
      this.loadProjConfig().then((projConfigRes) => {
        if (projConfigRes.invite_only === 'true') {
          this.canAccess = false;
          UsersService.canAccess(this.projectId, this.userIdParam).then((res) => {
            this.canAccess = res === true;
            this.checkingAccess = false;
          }).finally(() => {
            this.$nextTick(() => {
              this.loadClientDisplay();
            });
          });
        } else {
          this.checkingAccess = false;
        }
      }).finally(() => {
        this.$nextTick(() => {
          this.loadClientDisplay();
        });
      });
    },
    computed: {
      configuration() {
        return {
          projectId: this.projectId,
          authenticator: this.authenticator,
          serviceUrl: this.serviceUrl,
        };
      },
      serviceUrl() {
        return window.location.origin;
      },
      authenticator() {
        if (this.$store.getters.isPkiAuthenticated) {
          return 'pki';
        }
        return `${this.serviceUrl}/admin/projects/${encodeURIComponent(this.projectId)}/token/${encodeURIComponent(this.userIdParam)}`;
      },
    },
    methods: {
      loadClientDisplay() {
        if (document.querySelector('#skills-client-container') && this.canAccess && !this.displayLoaded) {
          const clientDisplay = new SkillsDisplayJS({
            version: this.selectedVersion,
            options: this.configuration,
            theme: this.theme,
            userId: this.userIdParam,
          });
          clientDisplay.attachTo(document.querySelector('#skills-client-container'));
          this.clientDisplay = clientDisplay;
          this.displayLoaded = true;
        }
      },
      versionChanged(newValue) {
        const maxVersion = Math.max(...this.versionOptions);
        if (maxVersion !== newValue) {
          SkillsReporter.reportSkill('VisitClientDisplayForEarlierVersion');
        }
      },
    },
  };
</script>

<style scoped>

</style>
