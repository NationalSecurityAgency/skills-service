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
      <div class="row px-3 pt-3">
        <div class="col-12">
          <b-form-group label="Projects Filter" label-class="text-muted">
            <b-input v-model="table.filter.name" v-on:keyup.enter="applyFilters"
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

      <skills-b-table :options="table.options" :items="projectsInternal"
                      data-cy="projectsTable">

        <template v-slot:cell(name)="data">
          <div class="row">
            <div class="col">
              <router-link :data-cy="`manageProjLink_${data.item.projectId}`" tag="a" :to="{ name:'Subjects', params: { projectId: data.item.projectId, project: data.item }}"
                           :aria-label="`Manage Project ${data.item.name}  via link`">
                <div class="h5">{{ data.item.name }}</div>
              </router-link>

              <div class="text-muted" style="font-size: 0.9rem;">ID: {{ data.item.projectId }}</div>
            </div>
            <div class="col-auto ml-auto mr-0">
              <router-link :data-cy="`manageProjBtn_${data.item.projectId}`" :to="{ name:'Subjects', params: { projectId: data.item.projectId, project: data.item }}"
                           :aria-label="`Manage Project ${data.item.name}`"
                           class="btn btn-outline-primary btn-sm">
                <span class="d-none d-sm-inline">Manage </span> <i class="fas fa-arrow-circle-right" aria-hidden="true"/>
              </router-link>
              <b-button-group size="sm" class="ml-1">
                <b-button @click="editProject(data.item)"
                          variant="outline-primary" :data-cy="`editProjectId${data.item.projectId}`"
                          :aria-label="'edit Project '+data.item.name" :ref="'edit_'+data.item.projectId">
                  <i class="fas fa-edit" aria-hidden="true"/>
                </b-button>
                <b-button @click="deleteProject(data.item)" variant="outline-primary"
                          :data-cy="`deleteProjectButton_${data.item.projectId}`"
                          :aria-label="'delete Project '+data.item.name">
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
        <template v-slot:cell(lastReportedSkill)="data">
          <slim-date-cell :value="data.value" :fromStartOfDay="true"/>
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
  </div>

</template>

<script>
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import ProjectService from './ProjectService';
  import SkillsBTable from '../utils/table/SkillsBTable';
  import SlimDateCell from '../utils/table/SlimDateCell';
  import dayjs from '../../DayJsCustomizer';

  export default {
    name: 'MyProjects',
    mixins: [MsgBoxMixin],
    props: ['projects'],
    data() {
      return {
        currentlyFocusedProjectId: '',
        projectsInternal: [],
        projectsOriginal: [],
        table: {
          options: {
            busy: true,
            bordered: false,
            outlined: true,
            stacked: 'md',
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
                key: 'lastReportedSkill',
                label: 'Last Reported Skill',
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
          },
          filter: {
            name: '',
          },
        },
      };
    },
    components: {
      SkillsBTable,
      SlimDateCell,
    },
    mounted() {
      this.projectsInternal = this.projects.map((item) => item);
      this.projectsOriginal = this.projects.map((item) => item);
      this.table.options.pagination.totalRows = this.projects.length;
      this.table.options.busy = false;
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
      deleteProject(project) {
        ProjectService.checkIfProjectBelongsToGlobalBadge(project.projectId)
          .then((belongsToGlobal) => {
            if (belongsToGlobal) {
              const msg = 'Cannot delete this project as it belongs to one or more global badges. Please contact a Supervisor to remove this dependency.';
              this.msgOk(msg, 'Unable to delete');
            } else {
              const msg = `Project ID [${project.projectId}]. Delete Action can not be undone and permanently removes its skill subject definitions, skill definitions and users' performed skills.`;
              this.msgConfirm(msg)
                .then((res) => {
                  if (res) {
                    this.$emit('project-deleted', project);
                  }
                });
            }
          });
      },
      editProject(projectToEdit) {
        this.currentlyFocusedProjectId = projectToEdit.projectId;
        this.$emit('edit-project', projectToEdit);
      },
    },
  };
</script>
