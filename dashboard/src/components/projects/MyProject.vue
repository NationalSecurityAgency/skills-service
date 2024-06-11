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
  <div data-cy="projectCard" class="h-100">
    <div class="card h-100" :data-cy="`projectCard_${projectInternal.projectId}`">
      <div class="card-body">
        <div class="row mb-2">
          <div class="col-md text-truncate">
            <router-link
              :to="{ name:'Subjects', params: { projectId: this.projectInternal.projectId, project: this.projectInternal }}"
              class="text-info mb-0 pb-0 preview-card-title" :title="`${projectInternal.name}`"
              :aria-label="`manage project ${this.projectInternal.name}`"
              role="link"
              :data-cy="`projCard_${projectInternal.projectId}_manageLink`">
              <b-avatar variant="info" icon="people-fill" class="text-uppercase avatar-link" aria-hidden="true">
                {{ projectInternal.name.substring(0, 2) }}
              </b-avatar>
              {{ projectInternal.name }}
            </router-link>
            <div v-if="projectInternal.userCommunity" class="my-2" data-cy="userCommunity">
              <span class="border p-1 border-danger rounded"><i
                class="fas fa-shield-alt text-danger" aria-hidden="true"/></span> <span
              class="text-secondary font-italic ml-1">{{ beforeCommunityLabel }}</span> <span
              class="font-weight-bold text-primary">{{ projectInternal.userCommunity }}</span> <span
              class="text-secondary font-italic">{{ afterCommunityLabel }}</span>
            </div>
          </div>
          <div class="col-md-auto mt-3 mt-md-0">
            <project-card-controls
              :class="{ 'mr-md-4': !disableSortControl}"
              ref="cardControls"
              :project="projectInternal"
              @edit-project="editProject"
              @copy-project="copyProject"
              @delete-project="deleteProject"
              @unpin-project="unpin"
              :read-only-project="isReadOnlyProj"
              :is-delete-disabled="deleteProjectDisabled"
              :delete-disabled-text="deleteProjectToolTip"/>
          </div>
        </div>

        <div class="row text-center justify-content-center">
          <div v-for="(stat) in stats" :key="stat.label" class="col mt-1" style="min-width: 10rem;">
            <div :data-cy="`pagePreviewCardStat_${stat.label}`"
                 class="border rounded stat-card h-100">
              <i :class="stat.icon"></i>
              <div class="text-uppercase text-muted count-label">{{ stat.label }}</div>
              <strong class="h4" data-cy="statNum">{{ stat.count | number }}</strong>
              <i v-if="stat.warn" class="fas fa-exclamation-circle text-warning ml-1"
                 style="font-size: 1.5rem;"
                 v-b-tooltip.hover="stat.warnMsg"
                 data-cy="warning"
                 role="alert"
                 :aria-label="`Warning: ${stat.warnMsg}`"/>

              <div v-if="stat.secondaryStats">
                <div v-for="secCount in stat.secondaryStats" :key="secCount.label">
                  <div v-if="secCount.count > 0" style="font-size: 0.9rem">
                    <b-badge :variant="`${secCount.badgeVariant}`"
                             :data-cy="`pagePreviewCardStat_${stat.label}_${secCount.label}`">
                      <span>{{ secCount.count }}</span>
                    </b-badge>
                    <span class="text-left text-uppercase ml-1"
                          style="font-size: 0.8rem">{{ secCount.label }}</span>
                  </div>
                </div>
              </div>

            </div>
          </div>
        </div>

        <div class="text-center mt-1">
          <project-card-footer class="mt-4" :project="projectInternal"/>
        </div>

        <div
          v-if="projectInternal.expiring" data-cy="projectExpiration" class="w-100 text-center alert-danger p-2 mt-2">
              <span class="mr-2" v-b-tooltip.hover="'This Project has not been used recently, ' +
               'it will  be deleted unless you explicitly retain it'">Project has not been used in over <b>{{this.$store.getters.config.expireUnusedProjectsOlderThan}} days</b> and will be deleted <b>{{ fromExpirationDate() }}</b>.</span>
          <b-button @click="keepIt" data-cy="keepIt" size="sm" variant="alert" :aria-label="'Keep Project '+ projectInternal.name">
            <span class="d-none d-sm-inline">Keep It</span> <b-spinner v-if="cancellingExpiration" small style="font-size:1rem"/><i v-if="!cancellingExpiration" :class="'fas fa-shield-alt'" style="font-size: 1rem;" aria-hidden="true"/>
          </b-button>
        </div>
      </div>

      <div v-if="!disableSortControl"
           :id="`sortControl_${this.project.projectId}`"
           ref="sortControl"
           @mouseover="overSortControl = true"
           @mouseleave="overSortControl = false"
           @keyup.down="moveDown"
           @keyup.up="moveUp"
           @click.prevent.self
           class="position-absolute text-secondary px-2 py-1 sort-control"
           tabindex="0"
           :aria-label="`Project Sort Control. Current position for ${project.name} project is ${project.displayOrder}. Press up or down to change the order of the project.`"
           role="button"
           data-cy="sortControlHandle"><i class="fas fa-arrows-alt"></i></div>
    </div>

    <edit-project id="editProjectModal" v-if="showEditProjectModal" v-model="showEditProjectModal"
                  :project="projectInternal" :is-edit="true"
                  @project-saved="projectSaved" @hidden="handleHidden"/>
    <edit-project id="copyProjectModal" v-if="copyProjectInfo.showModal"
                  v-model="copyProjectInfo.showModal"
                  :project="copyProjectInfo.newProject"
                  :is-edit="false"
                  :is-copy="true"
                  @project-saved="projectCopied"
                  @hidden="handleCopyModalIsHidden"/>

    <removal-validation v-if="deleteProjectInfo.showDialog" v-model="deleteProjectInfo.showDialog"
                        @do-remove="doDeleteProject" @hidden="handleDeleteCancelled">
      <p>
        This will remove <span
        class="text-primary font-weight-bold">{{ deleteProjectInfo.project.name }}</span>.
      </p>
      <div>
        Deletion can not be undone and permanently removes all skill subject definitions, skill
        definitions and users' performed skills for this Project.
      </div>
    </removal-validation>
  </div>
</template>

<script>
  import dayjs from '@/common-components/DayJsCustomizer';
  import ProjectCardControls from '@/components/projects/ProjectCardControls';
  import ProjectCardFooter from '@/components/projects/ProjectCardFooter';
  import RemovalValidation from '@/components/utils/modal/RemovalValidation';
  import EditProject from '@/components/projects/EditProject';
  import ProjectService from '@/components/projects/ProjectService';
  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';
  import SettingsService from '@/components/settings/SettingsService';
  import UserRolesUtil from '@/components/utils/UserRolesUtil';
  import CommunityLabelsMixin from '@/components/utils/CommunityLabelsMixin';

  export default {
    name: 'MyProject',
    components: {
      ProjectCardFooter,
      ProjectCardControls,
      EditProject,
      RemovalValidation,
    },
    props: ['project', 'disableSortControl'],
    mixins: [MsgBoxMixin, CommunityLabelsMixin],
    data() {
      return {
        isLoading: false,
        pinned: false,
        projectInternal: { ...this.project },
        stats: [],
        showEditProjectModal: false,
        deleteProjectDisabled: false,
        deleteProjectToolTip: '',
        cancellingExpiration: false,
        deleteProjectInfo: {
          showDialog: false,
          project: {},
        },
        copyProjectInfo: {
          showModal: false,
          newProject: {},
        },
      };
    },
    mounted() {
      this.pinned = this.projectInternal.pinned;
      this.createCardOptions();
    },
    computed: {
      minimumPoints() {
        return this.$store.getters.config.minimumProjectPoints;
      },
      isRootUser() {
        return this.$store.getters['access/isRoot'];
      },
      expirationDate() {
        if (!this.projectInternal.expiring) {
          return '';
        }
        const gracePeriodInDays = this.$store.getters.config.expirationGracePeriod;
        const expires = dayjs(this.projectInternal.expirationTriggered).add(gracePeriodInDays, 'day').startOf('day');
        return expires.format('YYYY-MM-DD HH:mm');
      },
      isReadOnlyProj() {
        return UserRolesUtil.isReadOnlyProjRole(this.projectInternal.userRole);
      },
    },
    methods: {
      fromExpirationDate() {
        return dayjs()
          .startOf('day')
          .to(dayjs(this.expirationDate));
      },
      handleHidden() {
        this.$nextTick(() => {
          this.$refs.cardControls.focusOnEdit();
        });
      },
      handleCopyModalIsHidden() {
        this.$nextTick(() => {
          if (this.$refs && this.$refs.cardControls) {
            this.$refs.cardControls.focusOnCopy();
          }
        });
      },
      createCardOptions() {
        this.stats = [{
          label: 'Subjects',
          count: this.projectInternal.numSubjects,
          icon: 'fas fa-cubes skills-color-subjects',
        }, {
          label: 'Skills',
          count: this.projectInternal.numSkills,
          icon: 'fas fa-graduation-cap skills-color-skills',
          secondaryStats: [{
            label: 'reused',
            count: this.projectInternal.numSkillsReused,
            badgeVariant: 'info',
          }, {
            label: 'disabled',
            count: this.projectInternal.numSkillsDisabled,
            badgeVariant: 'warning',
          }],
        }, {
          label: 'Points',
          count: this.projectInternal.totalPoints,
          warn: (this.projectInternal.totalPoints + this.projectInternal.totalPointsReused) < this.minimumPoints,
          warnMsg: 'Project has insufficient points assigned. Skills cannot be achieved until project has at least 100 points.',
          icon: 'far fa-arrow-alt-circle-up skills-color-points',
          secondaryStats: [{
            label: 'reused',
            count: this.projectInternal.totalPointsReused,
            badgeVariant: 'info',
          }],
        }, {
          label: 'Badges',
          count: this.projectInternal.numBadges,
          icon: 'fas fa-award skills-color-badges',
        }];
      },
      checkIfProjectBelongsToGlobalBadge() {
        ProjectService.checkIfProjectBelongsToGlobalBadge(this.projectInternal.projectId)
          .then((res) => {
            if (res) {
              this.deleteProjectDisabled = true;
              this.deleteProjectToolTip = 'Cannot delete this project as it belongs to one or more global badges. Please contact a Supervisor to remove this dependency.';
            }
          });
      },
      doDeleteProject() {
        ProjectService.checkIfProjectBelongsToGlobalBadge(this.deleteProjectInfo.project.projectId)
          .then((belongsToGlobal) => {
            if (belongsToGlobal) {
              const msg = 'Cannot delete this project as it belongs to one or more global badges. Please contact a Supervisor to remove this dependency.';
              this.msgOk(msg, 'Unable to delete');
            } else {
              this.$emit('project-deleted', this.deleteProjectInfo.project);
            }
          });
      },
      deleteProject() {
        this.deleteProjectInfo.project = this.projectInternal;
        this.deleteProjectInfo.showDialog = true;
      },
      editProject() {
        this.showEditProjectModal = true;
      },
      copyProject() {
        this.copyProjectInfo.newProject = { userCommunity: this.project.userCommunity };
        this.copyProjectInfo.showModal = true;
      },
      projectCopied(project) {
        this.$emit('copy-project', {
          originalProjectId: this.projectInternal.projectId,
          newProject: project,
        });
      },
      projectSaved(project) {
        this.isLoading = true;
        ProjectService.saveProject(project)
          .then((res) => {
            this.projectInternal = res;
            this.pinned = this.projectInternal.pinned;
            this.createCardOptions();
            this.$announcer.polite(`Project ${project.name} has been successfully edited`);
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      unpin() {
        SettingsService.unpinProject(this.projectInternal.projectId)
          .then(() => {
            this.projectInternal.pinned = false;
            this.pinned = false;
            this.$emit('pin-removed', this.projectInternal);
          });
      },
      keepIt() {
        this.cancellingExpiration = true;
        ProjectService.cancelUnusedProjectDeletion(this.projectInternal.projectId)
          .then(() => {
            this.projectInternal.expiring = false;
          })
          .finally(() => {
            this.cancellingExpiration = false;
          });
      },
      moveDown() {
        this.$emit('sort-changed-requested', {
          projectId: this.project.projectId,
          direction: 'down',
        });
      },
      moveUp() {
        this.$emit('sort-changed-requested', {
          projectId: this.project.projectId,
          direction: 'up',
        });
      },
      focusSortControl() {
        this.$refs.sortControl.focus();
      },
      handleDeleteCancelled() {
        this.$refs.cardControls.focusOnDelete();
      },
    },
  };
</script>

<style lang="scss" scoped>
  @import "../../assets/custom";

  .project-settings {
    position: relative;
    display: inline-block;
    float: right;
  }
  .buttons i {
    font-size: 0.9rem;
  }

  .preview-card-title {
    font-size: 1.4rem;
    font-weight: bold;
  }

  .preview-card-subTitle {
    font-size: 0.8rem;
  }

  .count-label {
    font-size: 0.9rem;
  }

  .avatar-link i {
    font-size: 2.5rem;
    display: inline-block;
  }

  .stat-card {
    background-color: #f8f9fa;
    padding: 1rem;
  }

  .sort-control {
    font-size: 1.3rem !important;
    color: #b3b3b3 !important;
    top: 0rem;
    right: 0rem;
    border-bottom: 1px solid #e8e8e8;
    border-left: 1px solid #e8e8e8;
    background-color: #fbfbfb !important;
    border-bottom-left-radius:.25rem!important
  }

  .sort-control:hover, .sort-control i:hover {
    cursor: grab !important;
    color: $info !important;
    font-size: 1.5rem;
  }

</style>
