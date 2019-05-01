<template>
  <div id="skillsTable">

    <sub-page-header title="Skills" action="Skill" @add-action="newSkill"/>


    <div class="card">
      <div class="card-body">
        <div v-if="isLoading" class="modal-card-body" style="height: 400px;">
        </div>

        <v-client-table :data="skills" :columns="skillsColumns"
                        :options="options" v-if="this.skills && this.skills.length" v-on:sorted="handleColumnSort">

          <div slot="name" slot-scope="props" class="field has-addons">
            <div>
              <div>{{ props.row.name }}</div>
              <div class="text-muted" style="font-size: 0.9rem;">ID: {{ props.row.skillId }}</div>
            </div>
          </div>

          <div slot="displayOrder" slot-scope="props">
            <span>{{props.row.displayOrder}}</span>

            <b-button-group size="sm" class="ml-1"
                            v-b-popover.hover="'Sorting controls are enabled only when Display Order column is sorted in the ascending order.'">
              <b-button @click="moveDisplayOrderDown(props.row)" variant="outline-info"
                        :disabled="!sortButtonEnabled || props.row.disabledDownButton || isMovingRows">
                <i class="fas fa-arrow-circle-down"/>
              </b-button>
              <b-button @click="moveDisplayOrderUp(props.row)" variant="outline-info"
                        :disabled="!sortButtonEnabled || props.row.disabledDownButton || isMovingRows">
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
              <b-button @click="addUser(props.row)" variant="outline-primary"><i class="fas fa-user-plus"/></b-button>
            </b-button-group>
            <router-link :to="{ name:'SkillPage',
                            params: { projectId: props.row.projectId, subjectId: props.row.subjectId, skillId: props.row.skillId }}"
                         class="btn btn-outline-primary btn-sm">
              Manage <i class="fas fa-arrow-circle-right"/>
            </router-link>
          </div>

          <div slot="child_row" slot-scope="props" class="skills-table-child-row">
            <ChildRowSkillsDisplay :project-id="projectId" :subject-id="subjectId"
                                   :parent-skill-id="props.row.skillId"></ChildRowSkillsDisplay>
          </div>
        </v-client-table>

        <no-content :should-display="!(this.skills && this.skills.length)" :title="'No Skills Yet'">
          <div slot="content" class="content" style="width: 100%;">
            <p class="has-text-centered">
              Create your first skill today by pressing
            </p>
            <p class="has-text-centered">
              <new-skill-items-buttons v-on:new-skill-item="newSkill"></new-skill-items-buttons>
            </p>
          </div>
        </no-content>
      </div>
    </div>
  </div>
</template>

<script>
  import EditSkill from './EditSkill';
  import AddUser from './AddUser';
  import NoContent from '../utils/NoContent';
  import NewSkillItemsButtons from './NewSkillItemsButtons';
  import ChildRowSkillsDisplay from './ChildRowSkillsDisplay';
  import SkillsService from './SkillsService';
  import ToastHelper from '../utils/ToastHelper';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';

  export default {
    name: 'SkillsTable',
    mixins: [MsgBoxMixin],
    props: ['projectId', 'subjectId', 'skillsProp'],
    components: {
      SubPageHeader,
      ChildRowSkillsDisplay,
      NewSkillItemsButtons,
      NoContent,
    },
    data() {
      return {
        isLoading: false,
        skills: [],
        skillsColumns: ['name', 'displayOrder', 'created', 'edit'],
        sortButtonEnabled: false,
        isMovingRows: false,
        options: {
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
            created: 'not_mobile',
          },
          columnsClasses: {
            edit: 'control-column',
            displayOrder: 'display-order-column',
            created: 'date-column',
            name: 'skills-table-skill-name',
          },
          sortable: ['displayOrder', 'created', 'skillId', 'name', 'pointIncrement', 'pointIncrementInterval', 'totalPoints'],
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
        const emptySkill = {
          skillId: '',
          projectId: this.projectId,
          subjectId: this.subjectId,
          name: '',
          pointIncrement: 10,
          pointIncrementInterval: 8,
          numPerformToCompletion: 10,
          description: null,
          helpUrl: null,
        };

        this.$modal.open({
          parent: this,
          component: EditSkill,
          hasModalCard: true,
          canCancel: false,
          width: 1300,
          props: {
            skill: emptySkill,
            projectId: this.projectId,
            subjectId: this.subjectId,
          },
          events: {
            'skill-created': this.skillCreatedOrUpdated,
          },
        });
      },
      editSkill(skillToEdit) {
        this.$modal.open({
          parent: this,
          component: EditSkill,
          hasModalCard: true,
          canCancel: false,
          width: 1300,
          props: {
            skill: skillToEdit,
            isEdit: true,
            projectId: this.projectId,
            subjectId: this.subjectId,
          },
          events: {
            'skill-created': this.skillCreatedOrUpdated,
          },
        });
      },

      skillCreatedOrUpdated(skill) {
        this.isLoading = true;
        const item1Index = this.skills.findIndex(item => item.id === skill.id);
        // if (item1Index >= 0) {
        //   this.$set(this.skills[item1Index], 'isEditLoading', true);
        // }
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
            }

            this.disableFirstAndLastButtons();

            this.isLoading = false;

            this.$emit('skills-change', skill.skillId);
            this.toast(`Saved '${skill.name}' skill.`);
          })
          .finally(() => {
            this.isLoading = false;
          });
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
            const index = this.skills.findIndex(item => item.id === skill.id);
            this.skills.splice(index, 1);

            this.rebuildDisplayOrder();
            this.disableFirstAndLastButtons();
            this.isLoading = false;
            this.$emit('skills-change', skill.skillId);

            this.toast(`Skill '${skill.name}' was removed.`);
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

      addUser(row) {
        this.$modal.open({
          parent: this,
          component: AddUser,
          hasModalCard: true,
          canCancel: false,
          width: 1110,
          props: {
            skillId: row.skillId,
            projectId: this.projectId,
            suggestions: [],
            isFetching: false,
            skillName: row.name,
          },
        });
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
        this.isMovingRows = true;

        const item1Index = this.skills.findIndex(item => item.skillId === row.skillId);
        this.$set(this.skills[item1Index], 'isMoving', true);

        SkillsService.updateSkill(row, actionToSubmit)
          .then((response) => {
            const item2Index = this.skills.findIndex(item => item.skillId === response.skillId);
            const tmpOrder = this.skills[item1Index].displayOrder;
            this.skills[item1Index].displayOrder = this.skills[item2Index].displayOrder;
            this.skills[item2Index].displayOrder = tmpOrder;

            this.disableFirstAndLastButtons();
            this.isMovingRows = false;
            this.$set(this.skills[item2Index], 'isMoving', false);
          })
          .finally(() => {
            this.isMovingRows = false;
            this.$set(this.skills[item1Index], 'isMoving', false);
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
      toast(msg, isErr) {
        this.$toast.open(ToastHelper.defaultConf(msg, isErr));
      },
    },
  };
</script>

<style>
  .type-column {
    width: 8rem;
  }

  .control-column {
    width: 14rem;
    /*background: yellow;*/
  }

  .display-order-column {
    width: 9rem;
  }

  .date-column {
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

  .points {
    font-size: 1.2rem;
    font-weight: bold;
    padding-left: 5px;
  }

  .skills-table-child-row {
    background-color: #f9f9f9;
    margin-left: 2rem;
    padding-left: 1rem;
    padding-top: 1rem;
    padding-bottom: 1rem;
    border-left: 0.5px solid #dbdbdb;
  }

  /*remove count on the bottom of the table*/
  #skillsTable .VuePagination__count {
    display: none;
  }

  .skills-table-hierarchy-type {
    font-size: 0.8rem;
    font-weight: bold;
  }

  .skills-table-skill-name {
    font-size: 1.1rem;
    font-weight: bold;
  }

  /* reduce the width of first colun that hots expand control*/
  #skillsTable tbody > tr > td:first-child {
    padding: 0.5rem 0rem;
  }

</style>
