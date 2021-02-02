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
    <page-preview-card :options="cardOptions">
      <div slot="header-top-right">
        <b-button v-if="isRootUser" class="mr-2" @click="unpin" data-cy="unpin" size="sm"
                  variant="outline-primary" :aria-label="'remove pin for project '+ projectInternal.name"
                  :aria-pressed="pinned">
          <span class="d-none d-sm-inline">Unpin</span> <i class="fas fa-ban" style="font-size: 1rem;" aria-hidden="true"/>
        </b-button>
        <edit-and-delete-dropdown ref="projectEditDeleteBtn" v-on:deleted="deleteProject" v-on:edited="editProject" v-on:move-up="moveUp"
                                  v-on:move-down="moveDown"
                                  :is-first="projectInternal.isFirst" :is-last="projectInternal.isLast"
                                  :is-loading="isLoading" :is-delete-disabled="deleteProjectDisabled" :delete-disabled-text="deleteProjectToolTip"
                                  class="project-settings"></edit-and-delete-dropdown>
      </div>
      <div slot="footer">
        <b-button :to="{ name:'Subjects', params: { projectId: this.projectInternal.projectId, project: this.projectInternal }}"
                  variant="outline-primary" :data-cy="`projCard_${this.projectInternal.projectId}_manageBtn`" :aria-label="`manage project ${this.projectInternal.name}`">
          Manage <i class="fas fa-arrow-circle-right" aria-hidden="true"/>
        </b-button>
      </div>
    </page-preview-card>

    <edit-project v-if="showEditProjectModal" v-model="showEditProjectModal" :project="projectInternal" :is-edit="true"
      @project-saved="projectSaved" @hidden="handleHidden"/>
  </div>
</template>

<script>
  import EditAndDeleteDropdown from '../utils/EditAndDeleteDropdown';
  import EditProject from './EditProject';
  import ProjectService from './ProjectService';
  import PagePreviewCard from '../utils/pages/PagePreviewCard';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import SettingsService from '../settings/SettingsService';

  export default {
    name: 'MyProject',
    components: { EditProject, PagePreviewCard, EditAndDeleteDropdown },
    props: ['project'],
    mixins: [MsgBoxMixin],
    data() {
      return {
        isLoading: false,
        pinned: false,
        projectInternal: { ...this.project },
        cardOptions: {},
        showEditProjectModal: false,
        deleteProjectDisabled: false,
        deleteProjectToolTip: '',
      };
    },
    mounted() {
      this.pinned = this.projectInternal.pinned;
      this.createCardOptions();
      // this.checkIfProjectBelongsToGlobalBadge();
    },
    computed: {
      minimumPoints() {
        return this.$store.getters.config.minimumProjectPoints;
      },
      isRootUser() {
        return this.$store.getters['access/isRoot'];
      },
    },
    watch: {
      isRootUser(newVal) {
        if (newVal) {
          this.enableSearch = true;
        }
      },
    },
    methods: {
      handleHidden() {
        this.$nextTick(() => {
          this.$refs.projectEditDeleteBtn.focus();
        });
      },
      createCardOptions() {
        this.cardOptions = {
          // icon: this.project.iconClass,
          title: this.projectInternal.name,
          subTitle: `ID: ${this.projectInternal.projectId}`,
          stats: [{
            label: 'Subjects',
            count: this.projectInternal.numSubjects,
          }, {
            label: 'Skills',
            count: this.projectInternal.numSkills,
          }, {
            label: 'Points',
            count: this.projectInternal.totalPoints,
            warn: this.projectInternal.totalPoints < this.minimumPoints,
            warnMsg: 'Project has insufficient points assigned. Skills cannot be achieved until project has at least 100 points.',
          }, {
            label: 'Badges',
            count: this.projectInternal.numBadges,
          }],
        };
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
    },
  };
</script>

<style lang="scss" scoped>
  .project-settings {
    position: relative;
    display: inline-block;
    float: right;
  }
</style>
