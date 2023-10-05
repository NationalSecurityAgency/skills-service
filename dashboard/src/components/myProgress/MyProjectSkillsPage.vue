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
    <div v-if="isLoadingSettings" class="d-flex justify-content-center mt-1">
      <b-spinner variant="primary" type="grow" label="Spinning"></b-spinner>
    </div>
    <div style="position: relative">
      <div v-if="isEmailEnabled && !isLoadingSettings && isSkillsDisplayHomePage"
           :class="{
              'contact-button-inline': isContactButtonInline,
              'w-100 text-right pr-3 pt-2 contact-button-on-top': !isContactButtonInline
           }">
        <b-button variant="outline-primary"
                  @click="showContactOwner" data-cy="contactOwnerBtn">
          Contact Project <i aria-hidden="true" class="fas fas fa-mail-bulk"/>
        </b-button>
      </div>
      <div v-if="!isLoadingSettings" id="skills-client-container" ref="skillsDisplayRef" @route-changed="skillsDisplayRouteChanged">
      </div>
    </div>
    <contact-owners-dialog v-if="showContact" :project-name="projectName" v-model="showContact" :project-id="projectId"/>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex';
  import { SkillsDisplayJS } from '@skilltree/skills-client-js';
  import MyProgressService from '@/components/myProgress/MyProgressService';
  import SkillsDisplayOptionsMixin from '@/components/myProgress/SkillsDisplayOptionsMixin';
  import SettingsService from '@/components/settings/SettingsService';
  import ProjectService from '@/components/projects/ProjectService';
  import ContactOwnersDialog from '@/components/myProgress/ContactOwnersDialog';

  export default {
    name: 'MyProjectSkillsPage',
    mixins: [SkillsDisplayOptionsMixin],
    components: {
      // SkillsDisplay,
      ContactOwnersDialog,
    },
    data() {
      return {
        isLoadingSettings: true,
        clientDisplay: null,
        windowWidth: 0,
        oneRem: 0,
        projectId: this.$route.params.projectId,
        projectDisplayName: 'PROJECT',
        projectName: 'Project',
        skillsVersion: 2147483647, // max int
        showContact: false,
        theme: {
          disableSkillTreeBrand: true,
          disableBreadcrumb: true,
          landingPageTitle: '',
          maxWidth: '100%',
          backgroundColor: '#f8f9fe',
          pageTitle: {
            textColor: '#212529',
            fontSize: '1.5rem',
            borderColor: '#dee2e6',
            borderStyle: 'none none solid none',
            backgroundColor: '#fff',
            textAlign: 'left',
            padding: '1.6rem 1rem 1.1rem 1rem',
            margin: '-10px -15px 1.6rem -15px',
          },
          backButton: {
            padding: '5px 10px',
            fontSize: '12px',
            lineHeight: '1.5',
          },
        },
        darkTheme: {
          disableSkillTreeBrand: false,
          disableBreadcrumb: false,
          maxWidth: '100%',
          backgroundColor: '#626d7d',
          pageTitle: {
            textColor: '#FFF',
            fontSize: '1.5rem',
          },
          textSecondaryColor: 'white',
          textPrimaryColor: 'white',
          stars: {
            unearnedColor: '#787886',
            earnedColor: 'gold',
          },
          progressIndicators: {
            beforeTodayColor: '#3e4d44',
            earnedTodayColor: '#667da4',
            completeColor: '#59ad52',
            incompleteColor: '#cdcdcd',
          },
          charts: {
            axisLabelColor: 'white',
          },
          tiles: {
            backgroundColor: '#152E4d',
            watermarkIconColor: '#a6c5f7',
          },
          buttons: {
            backgroundColor: '#152E4d',
            foregroundColor: '#59ad52',
          },
          graphLegendBorderColor: '1px solid grey',
        },
      };
    },
    created() {
      this.compute1Rem();
      window.addEventListener('resize', this.handleResize);
      this.handleResize();
    },
    mounted() {
      this.isLoadingSettings = true;
      SettingsService.getClientDisplayConfig(this.projectId).then((response) => {
        this.projectDisplayName = response.projectDisplayName?.toUpperCase();
        if (!this.$route.params.name) {
          MyProgressService.findProjectName(this.projectId).then((res) => {
            if (res) {
              this.$set(this.theme, 'landingPageTitle', `${this.projectDisplayName}: ${res.name}`);
              this.projectName = res.name;
            }
          });
        } else {
          this.$set(this.theme, 'landingPageTitle', `${this.projectDisplayName}: ${this.$route.params.name}`);
          this.projectName = this.$route.params.name;
        }
      })
        .finally(() => {
          this.isLoadingSettings = false;

          const clientDisplay = new SkillsDisplayJS({
            version: this.skillsVersion,
            options: this.options,
            theme: this.themeObj,
          });
          this.$nextTick(() => {
            clientDisplay.attachTo(document.querySelector('#skills-client-container'));
            this.clientDisplay = clientDisplay;
          });
        });
      this.handleProjInvitation();
    },
    computed: {
      ...mapGetters([
        'isEmailEnabled',
      ]),
      isContactButtonInline() {
        const currentLen = (this.projectName.length + this.projectDisplayName.length + 8) * 1.2;
        const titleWidthPx = currentLen * this.oneRem;
        return this.windowWidth > titleWidthPx;
      },
      isSkillsDisplayHomePage() {
        return this.skillsClientDisplayPath && (this.skillsClientDisplayPath.path === '/' || this.skillsClientDisplayPath.path === undefined);
      },
      themeObj() {
        if (this.$route.query.classicSkillsDisplay && this.$route.query.classicSkillsDisplay.toLowerCase() === 'true') {
          const res = { ...this.theme };
          return Object.assign(res, {
            disableSkillTreeBrand: false,
            disableBreadcrumb: false,
            pageTitle: {
              textColor: '#212529',
              fontSize: '1.5rem',
            },
          });
        }

        if (this.$route.query.enableTheme && this.$route.query.enableTheme.toLowerCase() === 'true') {
          const res = { ...this.theme };
          return Object.assign(res, this.darkTheme);
        }

        return this.theme;
      },
    },
    methods: {
      handleProjInvitation() {
        const isInvited = this.$route.query.invited;
        if (isInvited) {
          ProjectService.addToMyProjects(this.projectId);
        }
      },
      showContactOwner() {
        this.showContact = true;
      },
      handleResize() {
        this.windowWidth = window.innerWidth;
      },
      compute1Rem() {
        this.oneRem = parseFloat(getComputedStyle(document.documentElement).fontSize);
      },
    },
  };
</script>

<style scoped>
.contact-button-inline {
  position: absolute;
  right: 1rem;
  top: 1rem;
}

.contact-button-on-top {
  background-color: #fff !important;
}
</style>
