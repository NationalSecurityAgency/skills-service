<template>
  <div id="skillsTable">

    <sub-page-header title="Skills" action="Skill" @add-action="newSkill"/>


    <div class="card">
      <div class="card-body" style="min-height: 400px;">
        <div v-if="isLoading" class="modal-card-body">
        </div>

        <v-client-table class="vue-table-2" :data="skills" :columns="skillsColumns"
                        :options="options" v-if="this.skills && this.skills.length" v-on:sorted="handleColumnSort">

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
                        :disabled="!sortButtonEnabled || props.row.disabledDownButton">
                <i class="fas fa-arrow-circle-down"/>
              </b-button>
              <b-button @click="moveDisplayOrderUp(props.row)" variant="outline-info" :class="{disabled: props.row.disabledUpButton}"
                        :disabled="!sortButtonEnabled || props.row.disabledUpButton">
                <i class="fas fa-arrow-circle-up"/>
              </b-button>
            </b-button-group>
          </div>

          <div slot="created" slot-scope="props" class="field has-addons">
            {{ props.row.created }}
          </div>

          <div slot="edit" slot-scope="props">
            <b-button-group size="sm" class="mr-1">
              <b-button @click="editSkill(props.row)" variant="outline-primary"><i class="fas fa-edit"/></b-button>
              <b-button @click="deleteSkill(props.row)" variant="outline-primary"><i class="fas fa-trash"/></b-button>
            </b-button-group>
            <router-link :to="{ name:'SkillOverview',
                            params: { projectId: props.row.projectId, subjectId: props.row.subjectId, skillId: props.row.skillId }}"
                         class="btn btn-outline-primary btn-sm">
              <span class="d-none d-sm-inline">Manage </span> <i class="fas fa-arrow-circle-right"/>
            </router-link>
          </div>

          <div slot="child_row" slot-scope="props">
            <ChildRowSkillsDisplay :project-id="projectId" :subject-id="subjectId" v-skills-onMount="'ExpandSkillDetailsSkillsPage'"
                                   :parent-skill-id="props.row.skillId" class="mr-3 ml-5 mb-3"></ChildRowSkillsDisplay>
          </div>
        </v-client-table>

        <no-content2 v-if="!skills || skills.length==0" title="No Skills Yet" message="Start creating skills today!"/>
      </div>
    </div>

    <edit-skill v-if="editSkillInfo.show" v-model="editSkillInfo.show" :skillId="editSkillInfo.skill.skillId" :is-edit="editSkillInfo.isEdit"
                :project-id="projectId" :subject-id="subjectId" @skill-saved="skillCreatedOrUpdated"/>
  </div>
</template>

<script>
  import { SkillsReporter } from '@skills/skills-client-vue';
  import EditSkill from './EditSkill';
  import NoContent2 from '../utils/NoContent2';
  import ChildRowSkillsDisplay from './ChildRowSkillsDisplay';
  import SkillsService from './SkillsService';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import ToastSupport from '../utils/ToastSupport';

  export default {
    name: 'SkillsTable',
    mixins: [MsgBoxMixin, ToastSupport],
    props: ['projectId', 'subjectId', 'skillsProp'],
    components: {
      EditSkill,
      SubPageHeader,
      ChildRowSkillsDisplay,
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
            created: 'Created (GMT)',
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
      this.skills = this.skillsProp.map(item => Object.assign({ subjectId: this.subjectId }, item));
      this.disableFirstAndLastButtons();
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
        const item1Index = this.skills.findIndex(item => item.skillId === skill.originalSkillId);

        SkillsService.saveSkill(skill)
          .then((skillRes) => {
            let createdSkill = skillRes;
            // this hack is required because vue-tables-2 doesn't run its toMomentFormat
            // if object is added to the collection after the fact, sounds like a bug to me?
            createdSkill.created = window.moment(createdSkill.created);
            createdSkill = Object.assign({ subjectId: this.subjectId }, createdSkill);
            if (item1Index >= 0) {
              this.skills.splice(item1Index, 1, createdSkill);
            } else {
              this.skills.push(createdSkill);
              // report CreateSkill on when new skill is created
              SkillsReporter.reportSkill('CreateSkill');
            }
            // attribute based skills should report on new or update operation
            this.reportSkills(createdSkill);

            this.disableFirstAndLastButtons();

            this.isLoading = false;

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
        this.msgConfirm(`Skill Id: [${row.skillId}]. Delete Action can not be undone and permanently removes users' performed skills.`)
          .then((res) => {
            if (res) {
              this.doDeleteSkill(row);
            }
          });
      },
      doDeleteSkill(skill) {
        this.isLoading = true;
        SkillsService.deleteSkill(skill)
          .then(() => {
            const index = this.skills.findIndex(item => item.skillId === skill.skillId);
            this.skills.splice(index, 1);

            this.rebuildDisplayOrder();
            this.disableFirstAndLastButtons();
            this.isLoading = false;
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
            tableData[i].displayOrder = i;
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
