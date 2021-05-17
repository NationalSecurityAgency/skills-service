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

    <sub-page-header ref="subPageHeader" title="Skills" action="Skill" @add-action="newSkill"
                     :disabled="addSkillDisabled" :disabled-msg="addSkillsDisabledMsg" aria-label="new skill">
      <b-button @click="displayEditSubject"
                ref="editSubjectButton"
                class="btn btn-outline-primary mr-1"
                size="sm"
                variant="outline-primary"
                data-cy="btn_edit-subject"
                :aria-label="'edit Skill '+subject.subjectId">
        <span class="d-none d-sm-inline">Edit </span> <i class="fas fa-edit" aria-hidden="true"/>
      </b-button>
      <b-button ref="actionButton" type="button" size="sm" variant="outline-primary"
                :class="{'btn':true, 'btn-outline-primary':true, 'disabled':addSkillDisabled}"
                v-on:click="newSkill" :aria-label="'new skill'"
                :data-cy="`btn_Skills`">
        <span class="d-none d-sm-inline">Skill </span> <i class="fas fa-plus-circle"/>
      </b-button>
      <i v-if="addSkillDisabled" class="fas fa-exclamation-circle text-warning ml-1" style="pointer-events: all; font-size: 1.5rem;" v-b-tooltip.hover="addSkillsDisabledMsg"/>
    </sub-page-header>

    <loading-container v-bind:is-loading="isLoading">
      <b-card v-if="this.skillsOriginal && this.skillsOriginal.length" body-class="p-0">
      <div class="row px-3 pt-3">
        <div class="col-12">
          <b-form-group label="Skill Filter" label-class="text-muted">
            <b-input v-model="table.filter.name" v-on:keyup.enter="applyFilters"
                     data-cy="skillsTable-skillFilter" aria-label="skill name filter"/>
          </b-form-group>
        </div>
        <div class="col-md">
        </div>
      </div>

      <div class="row pl-3 mb-3">
        <div class="col">
          <b-button variant="outline-info" @click="applyFilters" data-cy="users-filterBtn"><i class="fa fa-filter"/> Filter</b-button>
          <b-button variant="outline-info" @click="reset" class="ml-1" data-cy="users-resetBtn"><i class="fa fa-times"/> Reset</b-button>
        </div>
      </div>

      <div class="row mb-2">
        <div class="col"></div>
        <div class="col-auto text-right" data-cy="skillsTable-additionalColumns">
            <span class="text-secondary mr-2">Additional Columns:</span>
            <b-form-checkbox-group class="d-inline"
              id="skillsAdditionalColumns"
              v-model="table.extraColumns.selected"
              :options="table.extraColumns.options"
              name="Skills Table Additional Columns"
            ></b-form-checkbox-group>
        </div>
      </div>

      <skills-b-table :options="table.options" :items="skills"
                      data-cy="skillsTable"
                      @sort-changed="handleColumnSort">

        <template v-slot:cell(name)="data">
          <div class="row">
            <div class="col-auto pr-0">
              <b-button size="sm" @click="data.toggleDetails" class="mr-2 py-0 px-1 btn btn-info"
                        :aria-label="`Expand details for ${data.item.name}`"
                        :data-cy="`expandDetailsBtn_${data.item.skillId}`">
                <i v-if="data.detailsShowing" class="fa fa-minus-square" />
                <i v-else class="fa fa-plus-square" />
              </b-button>
            </div>
            <div class="col pl-0">
              <router-link :data-cy="`manageSkillLink_${data.item.skillId}`" tag="a" :to="{ name:'SkillOverview',
                                  params: { projectId: data.item.projectId, subjectId: subject.subjectId, skillId: data.item.skillId }}"
                           :aria-label="`Manage skill ${data.item.name}  via link`">
                <div class="h5">{{ data.item.name }}</div>
              </router-link>

              <div class="text-muted" style="font-size: 0.9rem;">ID: {{ data.item.skillId }}</div>
            </div>
            <div class="col-auto ml-auto mr-0">
              <router-link :data-cy="`manageSkillBtn_${data.item.skillId}`" :to="{ name:'SkillOverview',
                                  params: { projectId: data.item.projectId, subjectId:  subject.subjectId, skillId: data.item.skillId }}"
                           :aria-label="`Manage skill ${data.item.name}`"
                           class="btn btn-outline-primary btn-sm">
                <span class="d-none d-sm-inline">Manage </span> <i class="fas fa-arrow-circle-right" aria-hidden="true"/>
              </router-link>
              <b-button-group size="sm" class="ml-1">
                <b-button @click="copySkill(data.item)"
                          variant="outline-primary" :data-cy="`copySkillButton_${data.item.skillId}`"
                          :aria-label="'copy Skill '+data.item.name" :ref="'copy_'+data.item.skillId">
                  <i class="fas fa-copy" aria-hidden="true" />
                </b-button>
                <b-button @click="editSkill(data.item)"
                          variant="outline-primary" :data-cy="`editSkillButton_${data.item.skillId}`"
                          :aria-label="'edit Skill '+data.item.name" :ref="'edit_'+data.item.skillId">
                  <i class="fas fa-edit" aria-hidden="true"/>
                </b-button>
                <b-button @click="deleteSkill(data.item)" variant="outline-primary"
                          :data-cy="`deleteSkillButton_${data.item.skillId}`"
                          :aria-label="'delete Skill '+data.item.name">
                  <i class="text-warning fas fa-trash" aria-hidden="true"/>
                </b-button>
              </b-button-group>
            </div>
          </div>
        </template>

        <template v-slot:cell(totalPoints)="data">
          <div>{{ data.value }}</div>
          <div class="small text-secondary">{{ data.item.pointIncrement | number }} pts x {{ data.item.numPerformToCompletion | number }} repetitions</div>
        </template>

        <template v-slot:cell(timeWindow)="data">
          <div>{{ timeWindowTitle(data.item) }}
            <i v-if="!timeWindowHasLength(data.item)" class="fas fa-question-circle text-muted" v-b-tooltip.hover="`${timeWindowDescription(data.item)}`"></i>
          </div>
        </template>

        <template v-slot:cell(displayOrder)="data">
          <div class="row">
            <div class="col">
              <span>{{data.value}}</span>
            </div>
            <div class="col-auto">
              <b-button-group size="sm" class="ml-1"
                              :id="`mvBtnGrp_${data.item.skillId}`">
                <b-popover :target="`mvBtnGrp_${data.item.skillId}`" triggers="hover">
                  Sorting controls are enabled only when Display Order column is sorted in ascending order.
                </b-popover>
                <b-button @click="moveDisplayOrderDown(data.item)" variant="outline-info" :class="{disabled:data.item.disabledDownButton}"
                          :disabled="!sortButtonEnabled || data.item.disabledDownButton" :aria-label="'move '+data.item.name+' down in the display order'"
                          :data-cy="`orderMoveDown_${data.item.skillId}`">
                  <i class="fas fa-arrow-circle-down"/>
                </b-button>
                <b-button @click="moveDisplayOrderUp(data.item)" variant="outline-info" :class="{disabled: data.item.disabledUpButton}"
                          :disabled="!sortButtonEnabled || data.item.disabledUpButton"
                          :aria-label="'move '+data.item.name+' up in the display order'"
                          :data-cy="`orderMoveUp_${data.item.skillId}`">
                  <i class="fas fa-arrow-circle-up"/>
                </b-button>
              </b-button-group>
            </div>
          </div>
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
        <template v-slot:cell(selfReportingType)="data">
         {{ getSelfReportingTypePretty(data.item.selfReportingType) }}
        </template>
        <template #row-details="row">
            <ChildRowSkillsDisplay :project-id="projectId" :subject-id="subject.subjectId" v-skills-onMount="'ExpandSkillDetailsSkillsPage'"
                                   :parent-skill-id="row.item.skillId" :refresh-counter="row.item.refreshCounter"
                                   class="mr-3 ml-5 mb-3"></ChildRowSkillsDisplay>
        </template>
      </skills-b-table>

    </b-card>

      <no-content2 v-else title="No Skills Yet" class="mt-4"
                 message="Projects are composed of Subjects which are made of Skills and a single skill defines a training unit within the gamification framework."/>
    </loading-container>

    <edit-skill v-if="editSkillInfo.show" v-model="editSkillInfo.show" :skillId="editSkillInfo.skill.skillId" :is-copy="editSkillInfo.isCopy" :is-edit="editSkillInfo.isEdit"
                :project-id="projectId" :subject-id="subject.subjectId" @skill-saved="skillCreatedOrUpdated" @hidden="handleHide"/>

    <edit-subject v-if="showEditSubject" v-model="showEditSubject"
                  :subject="subject" @subject-saved="subjectEdited"
                  :is-edit="true"
                  @hidden="handleHideSubjectEdit"/>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import { SkillsReporter } from '@skilltree/skills-client-vue';
  import EditSkill from './EditSkill';
  import NoContent2 from '../utils/NoContent2';
  import ChildRowSkillsDisplay from './ChildRowSkillsDisplay';
  import SkillsService from './SkillsService';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import ToastSupport from '../utils/ToastSupport';
  import LoadingContainer from '../utils/LoadingContainer';
  import SkillsBTable from '../utils/table/SkillsBTable';
  import dayjs from '../../DayJsCustomizer';
  import TimeWindowMixin from './TimeWindowMixin';
  import SubjectsService from '../subjects/SubjectsService';
  import EditSubject from '../subjects/EditSubject';

  const { mapGetters, mapMutations } = createNamespacedHelpers('subjects');

  export default {
    name: 'SkillsTable',
    mixins: [MsgBoxMixin, ToastSupport, TimeWindowMixin],
    props: ['projectId', 'subjectId', 'skillsProp'],
    components: {
      SkillsBTable,
      EditSkill,
      SubPageHeader,
      ChildRowSkillsDisplay,
      LoadingContainer,
      NoContent2,
      EditSubject,
    },
    data() {
      return {
        isLoading: false,
        showEditSubject: false,
        currentlyFocusedSkillId: '',
        editSkillInfo: {
          isEdit: false,
          isCopy: false,
          show: false,
          skill: {},
        },
        skillsOriginal: [],
        skills: [],
        table: {
          extraColumns: {
            options: [{
              value: 'totalPoints',
              text: 'Points',
            }, {
              value: 'selfReportingType',
              text: 'Self Report',
            }, {
              value: 'timeWindow',
              text: 'Time Window',
            }, {
              value: 'version',
              text: 'Version',
            }],
            selected: [],
          },
          filter: {
            name: '',
          },
          options: {
            rowDetailsControls: false,
            busy: true,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'created',
            sortDesc: true,
            fields: [
              {
                key: 'name',
                label: 'Skill',
                sortable: true,
              },
              {
                key: 'displayOrder',
                label: 'Display Order',
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
        },
        sortButtonEnabled: false,
      };
    },
    watch: {
      'table.extraColumns.selected': function updateColumns(newList) {
        const extraColLookup = {
          totalPoints: {
            key: 'totalPoints',
            label: 'Points',
            sortable: true,
          },
          version: {
            key: 'version',
            label: 'Version',
            sortable: true,
          },
          timeWindow: {
            key: 'timeWindow',
            label: 'Time Window',
            sortable: false,
          },
          selfReportingType: {
            key: 'selfReportingType',
            label: 'Self Report Type',
            sortable: true,
          },
        };

        Object.keys(extraColLookup).forEach((key) => {
          if (newList.includes(key)) {
            this.table.options.fields.push(extraColLookup[key]);
          } else {
            this.table.options.fields = this.table.options.fields.filter((item) => item.key !== key);
          }
        });
      },
    },
    mounted() {
      const subjectId = this.subject ? this.subject.subjectId : this.subjectId;
      this.skills = this.skillsProp.map((item) => {
        const withSubjId = { subjectId, refreshCounter: 0, ...item };
        return SkillsService.enhanceWithTimeWindow(withSubjId);
      });
      this.skillsOriginal = this.skills.map((item) => item);
      this.disableFirstAndLastButtons();
      this.table.options.pagination.totalRows = this.skills.length;
      this.table.options.busy = false;
    },
    computed: {
      ...mapGetters([
        'subject',
      ]),
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
      ...mapMutations([
        'setSubject',
      ]),
      applyFilters() {
        if (this.table.filter.name && this.table.filter.name.length > 0) {
          this.skills = this.skillsOriginal.filter((item) => {
            const filter = this.table.filter.name.trim().toLowerCase();
            if (item.name.trim().toLowerCase().indexOf(filter) !== -1
              || item.skillId.trim().toLowerCase().indexOf(filter) !== -1) {
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
        this.skills = this.skillsOriginal.map((item) => item);
      },
      isToday(timestamp) {
        return dayjs(timestamp)
          .isSame(new Date(), 'day');
      },
      newSkill() {
        this.editSkillInfo = {
          skill: {},
          show: true,
          isEdit: false,
          isCopy: false,
        };
      },
      editSkill(skillToEdit) {
        this.currentlyFocusedSkillId = skillToEdit.skillId;
        this.editSkillInfo = { skill: { ...skillToEdit, subjectId: this.subject.subjectId }, show: true, isEdit: true };
      },
      copySkill(skillToCopy) {
        // deep copy skill to prevent any future conflicts
        this.editSkillInfo = {
          skill: { ...skillToCopy, subjectId: this.subject.subjectId },
          show: true,
          isCopy: true,
          isEdit: false,
        };
      },
      doneShowingLoading() {
        this.isLoading = false;
        this.table.options.busy = false;
      },
      skillCreatedOrUpdated(skill) {
        if (this.skillsOriginal.length === 0) {
          this.isLoading = true;
        } else {
          this.table.options.busy = true;
        }

        const item1Index = this.skills.findIndex((item) => item.skillId === skill.originalSkillId);
        const { isEdit } = skill;

        SkillsService.saveSkill(skill)
          .then((skillRes) => {
            let createdSkill = skillRes;
            createdSkill = { subjectId: this.subject.subjectId, ...createdSkill, created: new Date(createdSkill.created) };
            if (item1Index >= 0) {
              createdSkill.refreshCounter = this.skills[item1Index].refreshCounter + 1;
              this.skills.splice(item1Index, 1, createdSkill);
            } else {
              createdSkill.refreshCounter = 0;
              this.skills.push(createdSkill);
              this.skillsOriginal.push(createdSkill);
              // report CreateSkill on when new skill is created
              SkillsReporter.reportSkill('CreateSkill');
            }

            // attribute based skills should report on new or update operation
            this.reportSkills(createdSkill);

            this.disableFirstAndLastButtons();

            this.$emit('skills-change', skill.skillId);
            this.successToast('Skill Saved', `Saved '${skill.name}' skill.`);

            if (isEdit) {
              setTimeout(() => {
                this.handleFocus({ updated: true });
              }, 0);
            }
          })
          .finally(() => {
            this.doneShowingLoading();
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
        if (this.skillsOriginal.length === 1) {
          this.isLoading = true;
        } else {
          this.table.options.busy = true;
        }
        SkillsService.deleteSkill(skill)
          .then(() => {
            const index = this.skills.findIndex((item) => item.skillId === skill.skillId);
            this.skills.splice(index, 1);

            const skillsOriginalIndex = this.skillsOriginal.findIndex((item) => item.skillId === skill.skillId);
            this.skillsOriginal.splice(skillsOriginalIndex, 1);

            this.rebuildDisplayOrder();
            this.disableFirstAndLastButtons();
            this.$emit('skills-change', skill.skillId);

            this.successToast('Removed Skill', `Skill '${skill.name}' was removed.`);
          })
          .finally(() => {
            this.doneShowingLoading();
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
        if (param.sortBy === 'displayOrder' && !param.sortDesc) {
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
            SkillsService.getSubjectSkills(this.projectId, this.subject.subjectId).then((data) => {
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
      handleHide(e) {
        if (!e || !e.saved || (e.saved && !e.updated)) {
          this.handleFocus(e);
        }
      },
      handleFocus(e) {
        let ref = this.$refs.actionButton;
        if (e && e.updated && this.currentlyFocusedSkillId) {
          const refName = `edit_${this.currentlyFocusedSkillId}`;
          ref = this.$refs[refName];
        }
        this.currentlyFocusedSkillId = '';
        this.$nextTick(() => {
          if (ref) {
            ref.focus();
          }
        });
      },
      getSelfReportingTypePretty(selfReportingType) {
        return (selfReportingType === 'HonorSystem') ? 'Honor System' : selfReportingType;
      },
      displayEditSubject() {
        this.showEditSubject = true;
      },
      subjectEdited(subject) {
        SubjectsService.saveSubject(subject).then((resp) => {
          const origId = this.subject.subjectId;
          this.setSubject(resp);
          if (resp.subjectId !== origId) {
            this.$router.replace({ name: this.$route.name, params: { ...this.$route.params, subjectId: resp.subjectId } });
          }
        });
      },
      handleHideSubjectEdit() {
        this.showEditSubject = false;
        const ref = this.$refs.editSubjectButton;
        this.$nextTick(() => {
          if (ref) {
            ref.focus();
          }
        });
      },
    },
  };
</script>

<style>

</style>
