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
  <div ref="mainFocus">
    <page-header :loading="isLoading" :options="headerOptions">
      <div slot="banner" v-if="project && project.expiring && !isReadOnlyProj" data-cy="projectExpiration"
           class="w-100 text-center alert-danger p-2 mb-3">
          <span class="mr-2"
                aria-label="This Project has not been used recently, it will  be deleted unless you explicitly retain it"
                v-b-tooltip.hover="'This Project has not been used recently, it will  be deleted unless you explicitly retain it'">
            Project has not been used in over <b>{{ this.$store.getters.config.expireUnusedProjectsOlderThan }} days</b> and will be deleted <b>{{
              fromExpirationDate()
            }}</b>.
          </span>
        <b-button @click="keepIt" data-cy="keepIt" size="sm" variant="alert"
                  :aria-label="'Keep Project '+ project.name">
          <span class="d-none d-sm-inline">Keep It</span>
          <b-spinner v-if="cancellingExpiration" small style="font-size:1rem"/>
          <i v-if="!cancellingExpiration" :class="'fas fa-shield-alt'" style="font-size: 1rem;" aria-hidden="true"/>
        </b-button>
      </div>
      <div slot="subSubTitle" v-if="project">
        <b-button-group v-if="!isReadOnlyProj" class="mb-3" size="sm">
          <b-button @click="displayEditProject"
                    ref="editProjectButton"
                    class="btn btn-outline-primary"
                    variant="outline-primary"
                    data-cy="btn_edit-project"
                    :aria-label="'edit Project '+project.projectId">
            <span>Edit </span> <i class="fas fa-edit" aria-hidden="true"/>
          </b-button>
          <b-button target="_blank" v-if="project" :to="{ name:'MyProjectSkills', params: { projectId: project.projectId } }"
                    data-cy="projectPreview"
                    v-skills="'PreviewProjectClientDisplay'"
                    variant="outline-primary" :aria-label="'preview client display for project'+project.name">
            <span>Preview</span> <i class="fas fa-eye" style="font-size:1rem;" aria-hidden="true"/>
          </b-button>
          <b-button v-if="isProjConfigDiscoverable"
                    ref="shareProjectButton"
                    @click="copyAndDisplayShareProjInfo"
                    data-cy="shareProjBtn"
                    variant="outline-primary"
                    v-skills="'ShareProject'"
                    :aria-label="`Share ${project.name} with new users`">
            <span>Share</span> <i class="fas fa-share-alt" style="font-size:1rem;" aria-hidden="true"/>
          </b-button>
        </b-button-group>
        <div data-cy="projectCreated">
          <i class="fas fa-clock text-success header-status-icon" aria-hidden="true" /> <project-dates :created="project.created" :load-last-reported-date="true"/>
        </div>
        <div v-if="userProjRole">
          <i class="fas fa-user-shield text-success header-status-icon" aria-hidden="true" /> <span class="text-secondary font-italic small">Role:</span> <span class="small text-primary" data-cy="userRole">{{ userProjRole | userRole }}</span>
        </div>
      </div>
      <div slot="footer">
        <import-finalize-alert />
      </div>
    </page-header>

    <navigation v-if="!isLoading" :nav-items="navItems">
    </navigation>

    <edit-project v-if="editProject" v-model="editProject" :project="project" :is-edit="true"
                  @project-saved="projectSaved" @hidden="editProjectHidden"/>
    <project-share-modal v-if="shareProjModal" v-model="shareProjModal"
                           :share-url="shareUrl"
                           @hidden="focusOnShareButton"/>
  </div>

</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import dayjs from '@/common-components/DayJsCustomizer';
  import ProjectDates from '@/components/projects/ProjectDates';
  import ImportFinalizeAlert from '@/components/skills/catalog/ImportFinalizeAlert';
  import Navigation from '@/components/utils/Navigation';
  import PageHeader from '@/components/utils/pages/PageHeader';
  import EditProject from '@/components/projects/EditProject';
  import ProjectService from '@/components/projects/ProjectService';
  import ProjectShareModal from '@/components/projects/ProjectShareModal';
  import ProjConfigMixin from '@/components/projects/ProjConfigMixin';

  const { mapActions, mapGetters, mapMutations } = createNamespacedHelpers('projects');

  export default {
    name: 'ProjectPage',
    mixins: [ProjConfigMixin],
    components: {
      ProjectShareModal,
      ImportFinalizeAlert,
      ProjectDates,
      PageHeader,
      Navigation,
      EditProject,
    },
    data() {
      return {
        isLoadingData: true,
        cancellingExpiration: false,
        editProject: false,
        shareProjModal: false,
        shareUrl: '',
      };
    },
    mounted() {
      this.loadProjects();
    },
    computed: {
      ...mapGetters([
        'project',
      ]),
      isLoading() {
        return this.isLoadingData || this.isLoadingProjConfig;
      },
      navItems() {
        const items = [
          { name: 'Subjects', iconClass: 'fa-cubes skills-color-subjects', page: 'Subjects' },
          { name: 'Badges', iconClass: 'fa-award skills-color-badges', page: 'Badges' },
          { name: 'Self Report', iconClass: 'fa-laptop skills-color-selfreport', page: 'SelfReport' },
          { name: 'Dependencies', iconClass: 'fa-project-diagram skills-color-dependencies', page: 'FullDependencyGraph' },
        ];

        if (!this.isReadOnlyProj) {
          items.push({ name: 'Skill Catalog', iconClass: 'fa-book skills-color-skill-catalog', page: 'SkillsCatalog' });
          items.push({ name: 'Levels', iconClass: 'fa-trophy skills-color-levels', page: 'ProjectLevels' });
        }

        items.push({ name: 'Users', iconClass: 'fa-users skills-color-users', page: 'ProjectUsers' });
        items.push({ name: 'Metrics', iconClass: 'fa-chart-bar skills-color-metrics', page: 'ProjectMetrics' });

        if (!this.isReadOnlyProj) {
          items.push({ name: 'Contact Users', iconClass: 'fas fa-mail-bulk', page: 'EmailUsers' });
          items.push({ name: 'Issues', iconClass: 'fas fa-exclamation-triangle', page: 'ProjectErrorsPage' });
          items.push({ name: 'Access', iconClass: 'fa-shield-alt skills-color-access', page: 'ProjectAccess' });
          items.push({ name: 'Settings', iconClass: 'fa-cogs skills-color-settings', page: 'ProjectSettings' });
        }

        return items;
      },
      headerOptions() {
        if (!this.project || !this.projConfig) {
          return {};
        }
        let visibilityIcon = 'fas fa-lock-open';
        let visibilityDescription = 'Not Discoverable';
        let visibilityType = 'PUBLIC';
        if (this.isProjConfigInviteOnly) {
          visibilityDescription = 'Invite Only';
          visibilityIcon = 'fas fa-lock';
          visibilityType = 'PRIVATE';
        } else if (this.isProjConfigDiscoverable) {
          visibilityDescription = 'Discoverable';
        }

        const stats = [{
          label: 'Visibility',
          preformatted: `<div class="h5 font-weight-bold mb-0">${visibilityType}</div>`,
          secondaryPreformatted: `<div class="text-secondary text-uppercase text-truncate" style="font-size:0.8rem;margin-top:0.1em;">${visibilityDescription}</div>`,
          icon: `${visibilityIcon} skills-color-visibility`,
        }, {
          label: 'Skills',
          count: this.project.numSkills,
          secondaryStats: [{
            label: 'reused',
            count: this.project.numSkillsReused,
            badgeVariant: 'info',
          }, {
            label: 'disabled',
            count: this.project.numSkillsDisabled,
            badgeVariant: 'warning',
          }],
          icon: 'fas fa-graduation-cap skills-color-skills',
        }, {
          label: 'Points',
          count: this.project.totalPoints,
          warnMsg: this.project.totalPoints < this.minimumPoints ? 'Project has insufficient points assigned. Skills cannot be achieved until project has at least 100 points.' : null,
          icon: 'far fa-arrow-alt-circle-up skills-color-points',
          secondaryStats: [{
            label: 'reused',
            count: this.project.totalPointsReused,
            badgeVariant: 'info',
          }],
        }, {
          label: 'Badges',
          count: this.project.numBadges,
          icon: 'fas fa-award skills-color-badges',
        }];

        if (!this.isReadOnlyProj) {
          stats.push({
            label: 'Issues',
            count: this.project.numErrors,
            icon: 'fas fa-exclamation-triangle',
          });
        }

        return {
          icon: 'fas fa-list-alt skills-color-projects',
          title: `PROJECT: ${this.project.name}`,
          subTitle: `ID: ${this.project.projectId}`,
          stats,
        };
      },
      minimumPoints() {
        return this.$store.getters.config.minimumProjectPoints;
      },
      expirationDate() {
        if (!this.project.expiring) {
          return '';
        }
        const gracePeriodInDays = this.$store.getters.config.expirationGracePeriod;
        const expires = dayjs(this.project.expirationTriggered).add(gracePeriodInDays, 'day').startOf('day');
        return expires.format('YYYY-MM-DD HH:mm');
      },
    },
    methods: {
      ...mapActions([
        'loadProjectDetailsState',
      ]),
      ...mapMutations([
        'setProject',
      ]),
      copyAndDisplayShareProjInfo() {
        const host = window.location.origin;
        this.shareUrl = `${host}/progress-and-rankings/projects/${this.project.projectId}?invited=true`;
        navigator.clipboard.writeText(this.shareUrl).then(() => {
          this.shareProjModal = true;
        });
      },
      fromExpirationDate() {
        return dayjs().startOf('day').to(dayjs(this.expirationDate));
      },
      displayEditProject() {
        this.editProject = true;
      },
      loadProjects() {
        this.isLoadingData = true;
        if (this.$route.params.project) {
          this.setProject(this.$route.params.project);
          this.isLoadingData = false;
        } else {
          this.loadProjectDetailsState({ projectId: this.$route.params.projectId })
            .finally(() => {
              this.isLoadingData = false;
            });
        }
      },
      editProjectHidden() {
        this.editProject = false;
        this.$nextTick(() => {
          const ref = this.$refs.editProjectButton;
          if (ref) {
            ref.focus();
          }
        });
      },
      focusOnShareButton() {
        this.$nextTick(() => {
          const ref = this.$refs.shareProjectButton;
          if (ref) {
            ref.focus();
          }
        });
      },
      projectSaved(updatedProject) {
        ProjectService.saveProject(updatedProject).then((resp) => {
          const origProjId = this.project.projectId;
          this.setProject(resp);
          if (resp.projectId !== origProjId) {
            this.$router.replace({ name: this.$route.name, params: { ...this.$route.params, projectId: resp.projectId } });
            this.projectId = resp.projectId;
          }
          this.$nextTick(() => {
            this.$announcer.polite(`Project ${updatedProject.name} has been edited`);
          });
        });
      },
      keepIt() {
        this.cancellingExpiration = true;
        ProjectService.cancelUnusedProjectDeletion(this.$route.params.projectId).then(() => {
          this.loadProjects();
        }).finally(() => {
          this.cancellingExpiration = false;
        });
      },
    },
  };
</script>

<style scoped>
.header-status-icon {
  font-size: 0.9rem;
  width: 1.2rem;
}
</style>
