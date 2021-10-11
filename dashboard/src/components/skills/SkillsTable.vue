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

    <loading-container v-bind:is-loading="isLoading">
      <div v-if="this.skillsOriginal && this.skillsOriginal.length">
        <div v-if="showSearch">
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
        </div>

      <div class="row mb-2">
        <div class="col"></div>
        <div class="col-auto text-right" data-cy="skillsTable-additionalColumns">
            <span class="text-secondary mr-2">Additional Columns:</span>
            <b-form-checkbox-group
              :id="`skillsAdditionalColumns_${tableId}`"
              class="d-inline"
              @input="updateColumns"
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
          <div class="row" :data-cy="`nameCell_${data.item.skillId}`">
            <div class="col-auto pr-0">
              <b-button size="sm" @click="data.toggleDetails" class="mr-2 py-0 px-1 btn btn-info"
                        :aria-label="`Expand details for ${data.item.name}`"
                        :data-cy="`expandDetailsBtn_${data.item.skillId}`">
                <i v-if="data.detailsShowing" class="fa fa-minus-square" />
                <i v-else class="fa fa-plus-square" />
              </b-button>
            </div>
            <div class="col pl-0">
              <div v-if="data.item.isGroupType">
                <div class="text-success font-weight-bold">
                  <i class="fas fa-layer-group" aria-hidden="true"></i> <span class="text-uppercase">Group</span>
                  <b-badge variant="success" class="ml-2 text-uppercase">{{ data.item.numSkillsInGroup }} skills</b-badge>
                  <b-badge v-if="!data.item.enabled" variant="warning" class="ml-2 text-uppercase">Disabled</b-badge>
                </div>
                <div class="h5 text-primary"><span v-if="data.item.nameHtml" v-html="data.item.nameHtml"></span><span v-else>{{ data.item.name }}</span></div>
              </div>
              <div v-if="data.item.isSkillType">
                <router-link :data-cy="`manageSkillLink_${data.item.skillId}`" tag="a" :to="{ name:'SkillOverview',
                                    params: { projectId: data.item.projectId, subjectId: data.item.subjectId, skillId: data.item.skillId }}"
                             :aria-label="`Manage skill ${data.item.name}  via link`">
                  <div class="h5"><span v-if="data.item.nameHtml" v-html="data.item.nameHtml"></span><span v-else>{{ data.item.name }}</span></div>
                </router-link>
              </div>

              <div class="text-muted" style="font-size: 0.9rem;">ID: <span v-if="data.item.skillIdHtml" v-html="data.item.skillIdHtml"></span><span v-else>{{ data.item.skillId }}</span></div>
            </div>
            <div class="col-auto ml-auto mr-0">
              <router-link v-if="data.item.isSkillType"
                           :data-cy="`manageSkillBtn_${data.item.skillId}`" :to="{ name:'SkillOverview',
                                  params: { projectId: data.item.projectId, subjectId: data.item.subjectId, skillId: data.item.skillId }}"
                           :aria-label="`Manage skill ${data.item.name}`"
                           class="btn btn-outline-primary btn-sm">
                <span class="d-none d-sm-inline">Manage </span> <i class="fas fa-arrow-circle-right" aria-hidden="true"/>
              </router-link>
              <b-button-group size="sm" class="ml-1">
                <b-button v-if="data.item.type === 'Skill'"
                          @click="copySkill(data.item)"
                          variant="outline-primary" :data-cy="`copySkillButton_${data.item.skillId}`"
                          :aria-label="'copy Skill '+data.item.name" :ref="'copy_'+data.item.skillId"
                          title="Copy Skill">
                  <i class="fas fa-copy" aria-hidden="true" />
                </b-button>
                <b-button @click="editSkill(data.item)"
                          variant="outline-primary" :data-cy="`editSkillButton_${data.item.skillId}`"
                          :aria-label="'edit Skill '+data.item.name" :ref="'edit_'+data.item.skillId"
                          title="Edit Skill">
                  <i class="fas fa-edit" aria-hidden="true"/>
                </b-button>
                <b-button @click="deleteSkill(data.item)" variant="outline-primary"
                          :data-cy="`deleteSkillButton_${data.item.skillId}`"
                          :aria-label="'delete Skill '+data.item.name"
                          title="Delete Skill">
                  <i class="text-warning fas fa-trash" aria-hidden="true"/>
                </b-button>
              </b-button-group>
            </div>
          </div>
        </template>
        <template v-slot:cell(totalPoints)="data">
            <div>{{ data.value | number }}</div>
            <div v-if="data.item.isSkillType" class="small text-secondary">{{ data.item.pointIncrement | number }} pts x {{ data.item.numPerformToCompletion | number }} repetitions</div>
            <div v-if="data.item.isGroupType" class="small text-secondary">from {{ data.item.numberOfSkillsInGroup | number }} skills</div>
        </template>

        <template v-slot:cell(timeWindow)="data">
          <div v-if="data.item.isSkillType">{{ timeWindowTitle(data.item) }}
            <i v-if="!timeWindowHasLength(data.item)" class="fas fa-question-circle text-muted" v-b-tooltip.hover="`${timeWindowDescription(data.item)}`"></i>
          </div>
          <div v-if="data.item.isGroupType" class="text-secondary">
            N/A
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
          <span v-if="data.item.isSkillType">{{ getSelfReportingTypePretty(data.item.selfReportingType) }}</span>
          <span v-if="data.item.isGroupType" class="text-secondary">N/A</span>
        </template>
        <template #row-details="row">
            <child-row-skill-group-display v-if="row.item.isGroupType" :group="row.item"
                                           @group-changed="row.item = Object.assign(row.item, arguments[0])"/>
            <ChildRowSkillsDisplay v-if="row.item.isSkillType" :project-id="projectId" :subject-id="subjectId" v-skills-onMount="'ExpandSkillDetailsSkillsPage'"
                                   :parent-skill-id="row.item.skillId" :refresh-counter="row.item.refreshCounter"
                                   class="mr-3 ml-5 mb-3"></ChildRowSkillsDisplay>
        </template>
      </skills-b-table>

      </div>
      <no-content2 v-else title="No Skills Yet" class="my-5"
                 message="Projects are composed of Subjects which are made of Skills and a single skill defines a training unit within the gamification framework."/>
    </loading-container>

    <edit-skill v-if="editSkillInfo.show" v-model="editSkillInfo.show" :skillId="editSkillInfo.skill.skillId" :group-id="editSkillInfo.skill.groupId"
                :is-copy="editSkillInfo.isCopy" :is-edit="editSkillInfo.isEdit"
                :project-id="projectId" :subject-id="subjectId" @skill-saved="skillCreatedOrUpdated" @hidden="handleFocus"/>
    <edit-skill-group v-if="editGroupInfo.show" v-model="editGroupInfo.show" :group="editGroupInfo.group" :is-edit="editGroupInfo.isEdit"
                      @group-saved="skillCreatedOrUpdated" @hidden="handleFocus"/>
  </div>
</template>

<script>
  import { SkillsReporter } from '@skilltree/skills-client-vue';
  import dayjs from '@/common-components/DayJsCustomizer';
  import EditSkill from './EditSkill';
  import NoContent2 from '../utils/NoContent2';
  import ChildRowSkillsDisplay from './ChildRowSkillsDisplay';
  import SkillsService from './SkillsService';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import ToastSupport from '../utils/ToastSupport';
  import LoadingContainer from '../utils/LoadingContainer';
  import SkillsBTable from '../utils/table/SkillsBTable';
  import TimeWindowMixin from './TimeWindowMixin';
  import ChildRowSkillGroupDisplay from './ChildRowSkillGroupDisplay';
  import StringHighlighter from '@/common-components/utilities/StringHighlighter';
  import EditSkillGroup from './EditSkillGroup';

  export default {
    name: 'SkillsTable',
    mixins: [MsgBoxMixin, ToastSupport, TimeWindowMixin],
    props: {
      projectId: String,
      subjectId: String,
      skillsProp: Array,
      showSearch: {
        type: Boolean,
        default: true,
      },
      showHeader: {
        type: Boolean,
        default: true,
      },
      showPaging: {
        type: Boolean,
        default: true,
      },
      tableId: {
        type: String,
        default: 'skillsTable',
      },
    },
    components: {
      EditSkillGroup,
      ChildRowSkillGroupDisplay,
      SkillsBTable,
      EditSkill,
      ChildRowSkillsDisplay,
      LoadingContainer,
      NoContent2,
    },
    data() {
      return {
        isLoading: false,
        currentlyFocusedSkillId: '',
        editSkillInfo: {
          isEdit: false,
          isCopy: false,
          show: false,
          skill: {},
        },
        editGroupInfo: {
          isEdit: false,
          show: false,
          group: {},
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
              remove: !this.showPaging,
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
    mounted() {
      this.skills = this.skillsProp.map((item) => {
        const withSubjId = {
          subjectId: this.subjectId,
          refreshCounter: 0,
          isGroupType: item.type === 'SkillsGroup',
          isSkillType: item.type === 'Skill',
          ...item,
        };
        return SkillsService.enhanceWithTimeWindow(withSubjId);
      });
      this.skillsOriginal = this.skills.map((item) => item);
      this.disableFirstAndLastButtons();
      this.table.options.pagination.totalRows = this.skills.length;
      this.table.options.busy = false;
    },
    methods: {
      updateColumns(newList) {
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
      applyFilters() {
        if (this.table.filter.name && this.table.filter.name.length > 0) {
          const filter = this.table.filter.name.trim().toLowerCase();

          this.skills = this.skillsOriginal.filter((item) => {
            if (item.name.trim().toLowerCase().indexOf(filter) !== -1
              || item.skillId.trim().toLowerCase().indexOf(filter) !== -1) {
              return true;
            }
            return false;
          })?.map((item) => {
            const nameHtml = StringHighlighter.highlight(item.name, filter);
            const skillIdHtml = StringHighlighter.highlight(item.skillId, filter);
            return { nameHtml, skillIdHtml, ...item };
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
      editSkill(itemToEdit) {
        this.currentlyFocusedSkillId = itemToEdit.skillId;
        if (itemToEdit.isGroupType) {
          this.editGroupInfo = {
            isEdit: true,
            show: true,
            group: itemToEdit,
          };
        } else {
          this.editSkillInfo = { skill: itemToEdit, show: true, isEdit: true };
        }
      },
      copySkill(skillToCopy) {
        // deep copy skill to prevent any future conflicts
        this.editSkillInfo = {
          skill: skillToCopy,
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

        return SkillsService.saveSkill(skill)
          .then((skillRes) => {
            let createdSkill = skillRes;
            createdSkill = {
              subjectId: this.subjectId,
              isGroupType: skill.type === 'SkillsGroup',
              isSkillType: skill.type === 'Skill',
              ...createdSkill,
              created: new Date(createdSkill.created),
            };
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
                this.handleFocus();
              }, 0);
            }
            return createdSkill;
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
            this.$emit('skill-removed', skill);

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
      handleFocus() {
        let ref = null;
        if (this.currentlyFocusedSkillId) {
          const refName = `edit_${this.currentlyFocusedSkillId}`;
          ref = this.$refs[refName];
        }
        this.currentlyFocusedSkillId = '';
        if (ref) {
          this.$nextTick(() => {
            ref.focus();
          });
        }
      },
      getSelfReportingTypePretty(selfReportingType) {
        return (selfReportingType === 'HonorSystem') ? 'Honor System' : selfReportingType;
      },
    },
  };
</script>

<style>

</style>
