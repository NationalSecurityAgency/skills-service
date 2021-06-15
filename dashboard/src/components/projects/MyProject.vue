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
  <div data-cy="projectCard">
    <div class="card h-100" :data-cy="`projectCard_${projectInternal.projectId}`">
      <div class="card-body">
        <div class="row mb-2">
          <div class="col-md text-truncate">
            <router-link
              :to="{ name:'Subjects', params: { projectId: this.projectInternal.projectId, project: this.projectInternal }}"
              class="text-info mb-0 pb-0 preview-card-title" :title="`${projectInternal.name}`"
              data-cy="projCard_proj1_manageLink"><b-avatar variant="info" icon="people-fill" class="text-uppercase avatar-link"> {{ projectInternal.name.substring(0,2) }}</b-avatar> {{ projectInternal.name }}
            </router-link>
            <div class="text-secondary preview-card-subTitle mt-1 ml-1">ID: {{ projectInternal.projectId }}</div>
          </div>
          <div class="col-md-auto mt-3 mt-md-0">
            <project-card-controls
              ref="cardControls"
              :project="projectInternal"
              @edit-project="editProject"
              @delete-project="deleteProject"
              @move-up-project="moveUp"
              @move-down-project="moveDown"
              @unpin-project="unpin"
              :is-delete-disabled="deleteProjectDisabled"
              :delete-disabled-text="deleteProjectToolTip"/>
          </div>
        </div>

        <div class="row text-center justify-content-center">
          <div v-for="(stat) in stats" :key="stat.label" class="col my-3" style="min-width: 10rem;">
            <div :data-cy="`pagePreviewCardStat_${stat.label}`" class="border rounded stat-card">
              <i :class="stat.icon"></i>
              <p class="text-uppercase text-muted count-label">{{ stat.label }}</p>
              <strong class="h4" data-cy="statNum">{{ stat.count | number }}</strong>
              <i v-if="stat.warn" class="fas fa-exclamation-circle text-warning ml-1" style="font-size: 1.5rem;" v-b-tooltip.hover="stat.warnMsg" data-cy="warning"/>
            </div>
          </div>
        </div>

        <div class="text-center mt-1">
          <project-card-footer class="mt-4" :project="projectInternal"/>
        </div>

        <div v-if="projectInternal.expiring" data-cy="projectExpiration" class="w-100 text-center alert-danger p-2 mt-2">
              <span class="mr-2" v-b-tooltip.hover="'This Project has not been used recently, ' +
               'it will  be deleted unless you explicitly retain it'">Project has not been used in over <b>{{this.$store.getters.config.expireUnusedProjectsOlderThan}} days</b> and will be deleted <b>{{ fromExpirationDate() }}</b>.</span>
          <b-button @click="keepIt" data-cy="keepIt" size="sm" variant="alert" :aria-label="'Keep Project '+ projectInternal.name">
            <span class="d-none d-sm-inline">Keep It</span> <b-spinner v-if="cancellingExpiration" small style="font-size:1rem"/><i v-if="!cancellingExpiration" :class="'fas fa-shield-alt'" style="font-size: 1rem;" aria-hidden="true"/>
          </b-button>
        </div>
      </div>
    </div>

    <edit-project v-if="showEditProjectModal" v-model="showEditProjectModal" :project="projectInternal" :is-edit="true"
      @project-saved="projectSaved" @hidden="handleHidden"/>
  </div>
</template>

<script>
  import ProjectCardControls from '@/components/projects/ProjectCardControls';
  import ProjectCardFooter from '@/components/projects/ProjectCardFooter';
  import EditProject from './EditProject';
  import ProjectService from './ProjectService';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import SettingsService from '../settings/SettingsService';
  import dayjs from '../../DayJsCustomizer';

  export default {
    name: 'MyProject',
    components: {
      ProjectCardFooter,
      ProjectCardControls,
      EditProject,
    },
    props: ['project'],
    mixins: [MsgBoxMixin],
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
    },
    methods: {
      fromExpirationDate() {
        return dayjs().startOf('day').to(dayjs(this.expirationDate));
      },
      handleHidden() {
        this.$nextTick(() => {
          this.$refs.cardControls.focusOnEdit();
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
        }, {
          label: 'Points',
          count: this.projectInternal.totalPoints,
          warn: this.projectInternal.totalPoints < this.minimumPoints,
          warnMsg: 'Project has insufficient points assigned. Skills cannot be achieved until project has at least 100 points.',
          icon: 'far fa-arrow-alt-circle-up skills-color-points',
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
      deleteProject() {
        ProjectService.checkIfProjectBelongsToGlobalBadge(this.projectInternal.projectId)
          .then((belongsToGlobal) => {
            if (belongsToGlobal) {
              const msg = 'Cannot delete this project as it belongs to one or more global badges. Please contact a Supervisor to remove this dependency.';
              this.msgOk(msg, 'Unable to delete');
            } else {
              const msg = `Project ID [${this.projectInternal.projectId}]. Delete Action can not be undone and permanently removes its skill subject definitions, skill definitions and users' performed skills.`;
              this.msgConfirm(msg)
                .then((res) => {
                  if (res) {
                    this.$emit('project-deleted', this.projectInternal);
                  }
                });
            }
          });
      },
      editProject() {
        this.showEditProjectModal = true;
      },
      projectSaved(project) {
        this.isLoading = true;
        ProjectService.saveProject(project)
          .then((res) => {
            this.projectInternal = res;
            this.pinned = this.projectInternal.pinned;
            this.createCardOptions();
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      moveUp() {
        this.$emit('move-project-up', this.projectInternal);
      },
      moveDown() {
        this.$emit('move-project-down', this.projectInternal);
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
        ProjectService.cancelUnusedProjectDeletion(this.projectInternal.projectId).then(() => {
          this.projectInternal.expiring = false;
        }).finally(() => {
          this.cancellingExpiration = false;
        });
      },
    },
  };
</script>

<style lang="scss" scoped>
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

  i {
    font-size: 2.5rem;
    display: inline-block;
  }

  .stat-card {
    background-color: #f8f9fa;
    padding: 1rem;
  }

</style>
