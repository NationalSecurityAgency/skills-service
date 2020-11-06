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
  <div id="skillsTable">

    <sub-page-header title="Skills" action="Skill" @add-action="newSkill" :disabled="addSkillDisabled" :disabled-msg="addSkillsDisabledMsg"/>

    <loading-container v-bind:is-loading="isLoading">
      <div v-if="this.skills && this.skills.length" class="card">
        <div class="card-body" style="min-height: 400px;">

          <v-client-table class="vue-table-2" :data="skills" :columns="skillsColumns"
                          :options="options" v-on:sorted="handleColumnSort" ref="table">

            <div slot="name" slot-scope="props" class="field has-addons">
              <div>
                <h5>{{ props.row.name }}</h5>
                <div class="text-muted" style="font-size: 0.9rem;">ID: {{ props.row.skillId }}</div>
              </div>
            </div>

            <div slot="displayOrder" slot-scope="props">
              <span>{{props.row.displayOrder}}</span>

              <b-button-group size="sm" class="ml-1"
                              v-b-popover.hover="'Sorting controls are enabled only when Display Order column is sorted in the ascending order.'">
                <b-button @click="moveDisplayOrderDown(props.row)" variant="outline-info" :class="{disabled:props.row.disabledDownButton}"
                          :disabled="!sortButtonEnabled || props.row.disabledDownButton" :aria-label="'move '+props.row.name+' down in the display order'">
                  <i class="fas fa-arrow-circle-down"/>
                </b-button>
                <b-button @click="moveDisplayOrderUp(props.row)" variant="outline-info" :class="{disabled: props.row.disabledUpButton}"
                          :disabled="!sortButtonEnabled || props.row.disabledUpButton"
                          :aria-label="'move '+props.row.name+' up in the display order'">
                  <i class="fas fa-arrow-circle-up"/>
                </b-button>
              </b-button-group>
            </div>

            <div slot="created" slot-scope="props" class="field has-addons" data-cy="skillTableCellCreatedDate">
              {{ props.row.created | date }}
            </div>

            <div slot="edit" slot-scope="props">
              <b-button-group size="sm" class="mr-1">
                <b-button @click="editSkill(props.row)"
                          variant="outline-primary" data-cy="editSkillButton"
                          :aria-label="'edit Skill '+props.row.name">
                  <i class="fas fa-edit" aria-hidden="true"/>
                </b-button>
                <b-button @click="deleteSkill(props.row)" variant="outline-primary"
                          data-cy="deleteSkillButton"
                          :aria-label="'delete Skill '+props.row.name">
                  <i class="text-warning fas fa-trash" aria-hidden="true"/>
                </b-button>
              </b-button-group>
              <router-link :to="{ name:'SkillOverview',
                              params: { projectId: props.row.projectId, subjectId: props.row.subjectId, skillId: props.row.skillId }}"
                           class="btn btn-outline-primary btn-sm">
                <span class="d-none d-sm-inline">Manage </span> <i class="fas fa-arrow-circle-right" aria-hidden="true"/>
              </router-link>
            </div>

            <div slot="child_row" slot-scope="props">
              <ChildRowSkillsDisplay :project-id="projectId" :subject-id="subjectId" v-skills-onMount="'ExpandSkillDetailsSkillsPage'"
                                     :parent-skill-id="props.row.skillId" :refresh-counter="props.row.refreshCounter"
                                     class="mr-3 ml-5 mb-3"></ChildRowSkillsDisplay>
            </div>
          </v-client-table>
        </div>
      </div>

      <no-content2 v-else title="No Skills Yet" class="mt-4"
                   message="Projects are composed of Subjects which are made of Skills and a single skill defines a training unit within the gamification framework."/>
    </loading-container>

    <edit-skill v-if="editSkillInfo.show" v-model="editSkillInfo.show" :skillId="editSkillInfo.skill.skillId" :is-edit="editSkillInfo.isEdit"
                :project-id="projectId" :subject-id="subjectId" @skill-saved="skillCreatedOrUpdated"/>
  </div>
</template>

<script>
  import { SkillsReporter } from '@skilltree/skills-client-vue';
  import EditSkill from './EditSkill';
  import NoContent2 from '../utils/NoContent2';
  import ChildRowSkillsDisplay from './ChildRowSkillsDisplay';
  import SkillsService from './SkillsService';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import ToastSupport from '../utils/ToastSupport';
  import LoadingContainer from '../utils/LoadingContainer';

  export default {
    name: 'SkillsTable',
    mixins: [MsgBoxMixin, ToastSupport],
    props: ['projectId', 'subjectId', 'skillsProp'],
    components: {
      EditSkill,
      SubPageHeader,
      ChildRowSkillsDisplay,
      LoadingContainer,
      NoContent2,
    },
    data() {
      return {
        isLoading: false,
        editSkillInfo: {
          isEdit: false,
          show: false,
          skill: {},
        },
        skills: [],
        skillsColumns: ['name', 'displayOrder', 'created', 'edit'],
        sortButtonEnabled: false,
        options: {
          uniqueKey: 'skillId',
          headings: {
            created: 'Created',
            name: 'Skill Name',
            edit: '',
            displayOrder: 'Display Order',
          },
          dateColumns: ['created'],
          dateFormat: 'YYYY-MM-DD HH:mm',
          descOrderColumns: ['created'],
          orderBy: { column: 'created', ascending: false },
          columnsDisplay: {
            displayOrder: 'not_mobile',
            created: 'not_mobile',
          },
          columnsClasses: {
            edit: 'control-column',
            displayOrder: 'display-order-column',
            created: 'date-column',
            name: 'skills-table-skill-name',
          },
          sortable: ['displayOrder', 'created', 'name'],
          sortIcon: {
            base: 'fa fa-sort', up: 'fa fa-sort-up', down: 'fa fa-sort-down', is: 'fa fa-sort',
          },
          highlightMatches: true,
          skin: 'table is-striped is-fullwidth',
          filterable: true,
        },
      };
    },
    mounted() {
      this.skills = this.skillsProp.map((item) => ({ subjectId: this.subjectId, refreshCounter: 0, ...item }));
      this.disableFirstAndLastButtons();
    },
    computed: {
      addSkillDisabled() {
        return this.skills && this.$store.getters.config && this.skills.length >= this.$store.getters.config.maxSkillsPerSubject;
      },
      addSkillsDisabledMsg() {
        if (this.$store.getters.config) {
          return `The maximum number of Skills allowed is ${this.$store.getters.config.maxSkillsPerSubject}`;
        }
        return '';
      },
    },
    methods: {
      newSkill() {
        this.editSkillInfo = {
          skill: {},
          show: true,
          isEdit: false,
        };
      },
      editSkill(skillToEdit) {
        this.editSkillInfo = { skill: skillToEdit, show: true, isEdit: true };
      },

      skillCreatedOrUpdated(skill) {
        this.isLoading = true;
        const item1Index = this.skills.findIndex((item) => item.skillId === skill.originalSkillId);
        SkillsService.saveSkill(skill)
          .then((skillRes) => {
            let createdSkill = skillRes;
            createdSkill = { subjectId: this.subjectId, ...createdSkill, created: new Date(createdSkill.created) };
            if (item1Index >= 0) {
              createdSkill.refreshCounter = this.skills[item1Index].refreshCounter + 1;
              this.skills.splice(item1Index, 1, createdSkill);
            } else {
              createdSkill.refreshCounter = 0;
              this.skills.push(createdSkill);
              // report CreateSkill on when new skill is created
              SkillsReporter.reportSkill('CreateSkill');
            }

            // attribute based skills should report on new or update operation
            this.reportSkills(createdSkill);

            this.disableFirstAndLastButtons();

            this.$emit('skills-change', skill.skillId);
            this.successToast('Skill Saved', `Saved '${skill.name}' skill.`);
          })
          .finally(() => {
            this.isLoading = false;
          });
      },

      reportSkills(createdSkill) {
        if (createdSkill.pointIncrementInterval <= 0) {
          SkillsReporter.reportSkill('CreateSkillDisabledTimeWindow');
        }
        if (createdSkill.numMaxOccurrencesIncrementInterval > 1) {
          SkillsReporter.reportSkill('CreateSkillMaxOccurrencesWithinTimeWindow');
        }
        if (createdSkill.helpUrl) {
          SkillsReporter.reportSkill('CreateSkillHelpUrl');
        }
        if (createdSkill.version > 0) {
          SkillsReporter.reportSkill('CreateSkillVersion');
        }
      },

      deleteSkill(row) {
        SkillsService.checkIfSkillBelongsToGlobalBadge(row.projectId, row.skillId)
          .then((belongsToGlobalBadge) => {
            if (belongsToGlobalBadge) {
              this.msgOk(`Cannot delete Skill Id: [${row.skillId}].  This skill belongs to one or more global badges. Please contact a Supervisor to remove this dependency.`, 'Unable to delete');
            } else {
              this.msgConfirm('Delete Action can NOT be undone and permanently removes users\' performed skills and any dependency associations.', `DELETE [${row.skillId}]?`)
                .then((res) => {
                  if (res) {
                    this.doDeleteSkill(row);
                  }
                });
            }
          });
      },
      doDeleteSkill(skill) {
        this.isLoading = true;
        SkillsService.deleteSkill(skill)
          .then(() => {
            const index = this.skills.findIndex((item) => item.skillId === skill.skillId);
            this.skills.splice(index, 1);

            this.rebuildDisplayOrder();
            this.disableFirstAndLastButtons();
            this.$emit('skills-change', skill.skillId);

            this.successToast('Removed Skill', `Skill '${skill.name}' was removed.`);
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      rebuildDisplayOrder() {
        if (this.skills && this.skills.length > 0) {
          // rebuild the display order from 0..N
          const tableData = this.skills.sort((a, b) => a.displayOrder - b.displayOrder);
          for (let i = 0; i < tableData.length; i += 1) {
            tableData[i].displayOrder = i + 1;
          }
        }
      },

      handleColumnSort(param) {
        if (param.column === 'displayOrder' && param.ascending) {
          this.sortButtonEnabled = true;
        } else {
          this.sortButtonEnabled = false;
        }

        this.disableFirstAndLastButtons();
      },
      moveDisplayOrderUp(row) {
        this.moveDisplayOrder(row, 'DisplayOrderUp');
      },
      moveDisplayOrderDown(row) {
        this.moveDisplayOrder(row, 'DisplayOrderDown');
      },
      moveDisplayOrder(row, actionToSubmit) {
        SkillsService.updateSkill(row, actionToSubmit)
          .then(() => {
            SkillsService.getSubjectSkills(this.projectId, this.subjectId).then((data) => {
              this.skills = data;
              this.disableFirstAndLastButtons();
            });
          });
      },
      disableFirstAndLastButtons() {
        if (this.skills && this.skills.length > 0) {
          const tableData = this.skills.sort((a, b) => a.displayOrder - b.displayOrder);
          for (let i = 0; i < tableData.length; i += 1) {
            tableData[i].disabledUpButton = false;
            tableData[i].disabledDownButton = false;
          }

          tableData[0].disabledUpButton = true;
          tableData[tableData.length - 1].disabledDownButton = true;
        }
      },
    },
  };
</script>

<style>
  #skillsTable .type-column {
    width: 8rem;
  }

  #skillsTable .control-column {
    width: 12rem;
  }

  #skillsTable .display-order-column {
    width: 9rem;
  }

  #skillsTable .date-column {
    width: 11rem;
  }

  .VueTables__child-row-toggler {
    width: 16px;
    height: 16px;
    line-height: 16px;
    display: block;
    margin: auto;
    text-align: center;
  }

  .VueTables__child-row-toggler--closed::before {
    font-family: "Font Awesome 5 Free";
    content: "\f0fe";
  }

  .VueTables__child-row-toggler--open::before {
    font-family: "Font Awesome 5 Free";
    content: "\f146";
  }

  /*remove count on the bottom of the table*/
  #skillsTable .VuePagination__count {
    display: none;
  }

  /* reduce the width of first column that hosts expand control*/
  #skillsTable tbody > tr > td:first-child {
    padding: 1rem 0rem;
    width: 2rem;
  }

  /* Work around - "Filter:" label is not left aligned */
  #skillsTable .form-inline label {
    justify-content: left !important;
  }

</style>
