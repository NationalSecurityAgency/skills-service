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
  <b-card body-class="p-0">
      <div class="row px-3 pt-3">
        <div class="col-12">
          <b-form-group label="Projects Filter" label-class="text-muted">
            <b-input v-model="table.filter.name" v-on:keydown.enter="applyFilters"
                     data-cy="projectsTable-projectFilter" aria-label="project name filter"/>
          </b-form-group>
        </div>
        <div class="col-md">
        </div>
      </div>

      <div class="row pl-3 mb-3">
        <div class="col">
          <b-button variant="outline-info" @click="applyFilters" data-cy="projectsTable-filterBtn"><i class="fa fa-filter"/> Filter</b-button>
          <b-button variant="outline-info" @click="reset" class="ml-1" data-cy="projectsTable-resetBtn"><i class="fa fa-times"/> Reset</b-button>
        </div>
      </div>

      <skills-b-table :options="table.options" :items="projectsInternal" tableStoredStateId="projectsTable"
                      data-cy="projectsTable">
        <template #head(name)="data">
          <span class="text-primary"><i class="fas fa-list-alt skills-color-projects" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(numSubjects)="data">
          <span class="text-primary"><i class="fas fa-cubes skills-color-subjects" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(numSkills)="data">
          <span class="text-primary"><i class="fas fa-graduation-cap skills-color-skills" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(totalPoints)="data">
          <span class="text-primary"><i class="far fa-arrow-alt-circle-up skills-color-points" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(numBadges)="data">
          <span class="text-primary"><i class="fas fa-award skills-color-badges" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(created)="data">
          <span class="text-primary"><i class="far fa-clock skills-color-events" aria-hidden="true"></i> {{ data.label }}</span>
        </template>

        <template v-slot:cell(name)="data">
          <div class="row" :id="`proj${data.item.projectId}`" tabindex="-1">
            <div class="col" :data-cy="`projCell_${data.item.projectId}`">
              <router-link :data-cy="`manageProjLink_${data.item.projectId}`" tag="a"
                           :to="{ name:'Subjects', params: { projectId: data.item.projectId, project: data.item }}"
                           :aria-label="`Manage Project ${data.item.name}  via link`">
                <div class="h5">{{ data.item.name }}</div>
              </router-link>

              <div class="text-muted" style="font-size: 0.9rem;">ID: {{ data.item.projectId }}</div>
              <div class="mt-2">
                <i class="fas fa-user-shield text-success" style="font-size: 1.05rem;" aria-hidden="true"></i> <i>Role:</i> <span data-cy="userRole">{{ data.item.userRole | userRole }}</span>
              </div>
            </div>
            <div class="col-auto ml-auto mr-0">
              <router-link :data-cy="`manageProjBtn_${data.item.projectId}`" :to="{ name:'Subjects', params: { projectId: data.item.projectId, project: data.item }}"
                           :aria-label="`Manage Project ${data.item.name}`"
                           class="btn btn-outline-primary btn-sm mr-2">
                <span class="d-none d-sm-inline">{{ isProjReadOnly(data.item) ? 'View' : 'Manage' }} </span> <i class="fas fa-arrow-circle-right" aria-hidden="true"/>
              </router-link>
              <b-button v-if="isRootUser" class="mr-2" @click="unpin(data.item)" data-cy="unpin" size="sm"
                        variant="outline-primary" :aria-label="'remove pin for project '+ data.item.name"
                        :aria-pressed="data.item.pinned">
                <span class="d-none d-sm-inline">Unpin</span> <i class="fas fa-ban" style="font-size: 1rem;" aria-hidden="true"/>
              </b-button>
              <b-button-group v-if="!isProjReadOnly(data.item)" size="sm" class="ml-0">
                <b-button @click="showProjectEditModal(data.item)"
                          variant="outline-primary" :data-cy="`editProjectId${data.item.projectId}`"
                          :aria-label="'edit Project '+data.item.name"
                          :ref="'edit_'+data.item.projectId">
                  <i class="fas fa-edit" aria-hidden="true"/>
                </b-button>
                <b-button @click="showProjectCopyModal(data.item)" :disabled="copyProjectDisabled"
                          variant="outline-primary" :data-cy="`copyProjectId${data.item.projectId}`"
                          :aria-label="'copy Project '+data.item.name"
                          :ref="'copy_'+data.item.projectId">
                  <i class="fas fa-copy" aria-hidden="true"/>
                </b-button>
                <b-button @click="deleteProject(data.item)" variant="outline-primary"
                          :data-cy="`deleteProjectButton_${data.item.projectId}`"
                          :aria-label="'delete Project '+data.item.name"
                          :ref="'delete_'+data.item.projectId">
                  <i class="text-warning fas fa-trash" aria-hidden="true"/>
                </b-button>
              </b-button-group>
            </div>
          </div>
        </template>

        <template v-slot:cell(numSubjects)="data">
          {{ data.value | number }}
        </template>
        <template v-slot:cell(numSkills)="data">
          {{ data.value | number }}
        </template>
        <template v-slot:cell(totalPoints)="data">
          {{ data.value | number }}
        </template>
        <template v-slot:cell(numBadges)="data">
          {{ data.value | number }}
        </template>
        <template v-slot:cell(numErrors)="data">
          {{ data.value | number }}
        </template>

        <template v-slot:cell(created)="data">
          <div>
            <span>{{ data.value | date }}</span>
            <b-badge v-if="isToday(data.value)" variant="info" class="ml-2">Today</b-badge>
          </div>
          <div class="text-muted small">
            {{ data.value | timeFromNow }}
          </div>
        </template>
      </skills-b-table>

    <removal-validation v-if="deleteProjectInfo.showDialog" v-model="deleteProjectInfo.showDialog"
                        @do-remove="doDeleteProject" @hidden="focusOnDeleteButton">
      <p>
        This will remove <span
        class="text-primary font-weight-bold">{{ deleteProjectInfo.project.name }}</span>.
      </p>
      <div>
        Deletion can not be undone and permanently removes all skill subject definitions, skill
        definitions and users'
        performed skills for this Project.
      </div>
    </removal-validation>

    <edit-project id="editProjectModal" v-if="editProject.show" v-model="editProject.show"
                  :project="editProject.project"
                  @project-saved="projectEdited" @hidden="handleProjectModalHide" :is-edit="true"/>
    <edit-project id="copyProjectModal" v-if="copyProjectInfo.show"
                  v-model="copyProjectInfo.show"
                  :project="copyProjectInfo.project"
                  :is-edit="false"
                  :is-copy="true"
                  @project-saved="projectCopied"
                  @hidden="handleCopyModalIsHidden"/>
  </b-card>

</template>

<script>
  import dayjs from '@/common-components/DayJsCustomizer';
  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';
  import ProjectService from '@/components/projects/ProjectService';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import RemovalValidation from '@/components/utils/modal/RemovalValidation';
  import EditProject from '@/components/projects/EditProject';
  import SettingsService from '@/components/settings/SettingsService';
  import UserRolesUtil from '@/components/utils/UserRolesUtil';

  export default {
    name: 'ProjectsTable',
    mixins: [MsgBoxMixin],
    props: ['projects', 'copyProjectDisabled'],
    data() {
      return {
        currentlyFocusedProjectId: '',
        projectsInternal: [],
        projectsOriginal: [],
        deleteProjectInfo: {
          showDialog: false,
          project: {},
        },
        editProject: {
          show: false,
          project: {},
        },
        copyProjectInfo: {
          show: false,
          project: {},
          originalProjectId: null,
        },
        table: {
          options: {
            busy: true,
            bordered: false,
            outlined: true,
            stacked: 'lg',
            tableDescription: 'Projects',
            fields: [
              {
                key: 'name',
                label: 'Project',
                sortable: true,
              },
              {
                key: 'numSubjects',
                label: 'Subjects',
                sortable: true,
              },
              {
                key: 'numSkills',
                label: 'Skills',
                sortable: true,
              },
              {
                key: 'totalPoints',
                label: 'Points',
                sortable: true,
              },
              {
                key: 'numBadges',
                label: 'Badges',
                sortable: true,
              },
              {
                key: 'created',
                label: 'Created',
                sortable: true,
              },
            ],
            pagination: {
              currentPage: 1,
              totalRows: 1,
              pageSize: 10,
              possiblePageSizes: [10, 15, 25],
            },
            sortBy: 'created',
            sortDesc: true,
          },
          filter: {
            name: '',
          },
        },
      };
    },
    components: {
      EditProject,
      SkillsBTable,
      RemovalValidation,
    },
    mounted() {
      this.projectsInternal = this.projects.map((item) => item);
      this.projectsOriginal = this.projects.map((item) => item);
      this.table.options.pagination.totalRows = this.projects.length;
      this.table.options.busy = false;
    },
    computed: {
      isRootUser() {
        return this.$store.getters['access/isRoot'];
      },
    },
    methods: {
      applyFilters() {
        if (this.table.filter.name && this.table.filter.name.length > 0) {
          this.projectsInternal = this.projectsOriginal.filter((item) => {
            const filter = this.table.filter.name.trim().toLowerCase();
            if (item.name.trim().toLowerCase().indexOf(filter) !== -1
              || item.projectId.trim().toLowerCase().indexOf(filter) !== -1) {
              return true;
            }
            return false;
          });
        } else {
          this.reset();
        }
      },
      reset() {
        this.table.filter.name = '';
        this.projectsInternal = this.projectsOriginal.map((item) => item);
      },
      isToday(timestamp) {
        return dayjs(timestamp)
          .isSame(new Date(), 'day');
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
      focusOnDeleteButton() {
        if (this.deleteProjectInfo.project) {
          const refId = `delete_${this.deleteProjectInfo.project.projectId}`;
          const ref = this.$refs[refId];
          this.$nextTick(() => {
            if (ref) {
              ref.focus();
            }
          });
        }
      },
      deleteProject(project) {
        this.deleteProjectInfo.project = project;
        this.deleteProjectInfo.showDialog = true;
      },
      showProjectEditModal(projectToEdit) {
        this.editProject.project = {
          ...projectToEdit,
          originalProjectId: projectToEdit.projectId,
          isEdit: true,
        };
        this.editProject.show = true;
      },
      showProjectCopyModal(projectToCopy) {
        this.copyProjectInfo.originalProjectId = projectToCopy.projectId;
        this.copyProjectInfo.show = true;
      },
      projectEdited(editedProject) {
        this.$emit('project-edited', editedProject);
      },
      projectCopied(project) {
        this.$emit('copy-project', {
          originalProjectId: this.copyProjectInfo.originalProjectId,
          newProject: project,
        });
      },
      handleProjectModalHide() {
        this.focusOnEditButton(this.editProject.project.projectId);
      },
      handleCopyModalIsHidden() {
        const refId = `copy_${this.copyProjectInfo.originalProjectId}`;
        const ref = this.$refs[refId];
        this.$nextTick(() => {
          if (ref) {
            ref.focus();
          }
        });
      },
      unpin(project) {
        SettingsService.unpinProject(project.projectId)
          .then(() => {
            this.$emit('pin-removed', project);
          });
      },
      focusOnEditButton(projectId) {
        const refId = `edit_${projectId}`;
        const ref = this.$refs[refId];
        this.$nextTick(() => {
          ref.focus();
        });
      },
      isProjReadOnly(proj) {
        return UserRolesUtil.isReadOnlyProjRole(proj.userRole);
      },
    },
  };
</script>
