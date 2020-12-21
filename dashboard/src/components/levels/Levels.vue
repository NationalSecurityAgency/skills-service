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
  <div id="level-def-panel">
    <sub-page-header title="Level Definitions">
      <div class="row">
        <div class="col">
          <b-tooltip target="remove-button" title="You must retain at least one level." :disabled="!onlyOneLevelLeft"></b-tooltip>
          <span id="remove-button" class="mr-2">
            <b-button variant="outline-primary" ref="removeNextLevel" @click="removeLastItem" :disabled="onlyOneLevelLeft" size="sm">
              <span class="d-none d-sm-inline">Remove</span> Highest <i class="text-warning fas fa-trash-alt" aria-hidden="true"/>
            </b-button>
          </span>
          <b-tooltip target="add-button" title="Reached maximum limit of levels." :disabled="!reachedMaxLevels"></b-tooltip>
          <span id="add-button">
            <b-button @click="editLevel()" ref="addLevel" variant="outline-primary" :disabled="reachedMaxLevels"
                      size="sm" data-cy="addLevel">
              <span class="d-none d-sm-inline">Add</span> Next <i class="fas fa-plus-circle" aria-hidden="true"/>
            </b-button>
          </span>
        </div>
      </div>
    </sub-page-header>

    <loading-container :is-loading="isLoading">
      <simple-card>
        <v-client-table v-if="levels && levels.length && !isLoading" :data="levels" :columns="levelsColumns"
                        :options="options" data-cy="levelsTable">
        <span slot="iconClass" slot-scope="props">
              <i class="text-primary level-icon" v-bind:class="`${props.row.iconClass}`"></i>
                <i v-if="props.row.achievable === false" class="icon-warning fa fa-exclamation-circle text-warning"
                   v-b-tooltip.hover="'Level is unachievable. Insufficient available points in project.'"/>
        </span>
          <span slot="pointsFrom" slot-scope="props">
          <span v-if="props.row.pointsFrom !== null">{{ props.row.pointsFrom | number }}</span>
          <span v-else>N/A - Please create more rules first</span>
        </span>
          <span slot="pointsTo" slot-scope="props">
          <span v-if="props.row.pointsTo">{{props.row.pointsTo | number}}</span>
          <span v-else-if="!props.row.pointsFrom">N/A - Please create more rules first</span>
          <span v-else><i class="fas fa-infinity"/></span>
        </span>

          <div slot="edit" slot-scope="props" class="">
            <b-button :ref="`edit_${props.row.level}`" @click="editLevel(props.row)" variant="outline-primary" style="width: 5rem;" data-cy="editLevelButton">
                      <i class="fas fa-edit"/> Edit
            </b-button>
          </div>
        </v-client-table>
      </simple-card>
    </loading-container>
    <new-level v-if="displayLevelModal"
               v-model="displayLevelModal"
               @new-level="doCreateNewLevel"
               @edited-level="doEditLevel"
               :boundaries="bounds"
               :level="levelToEdit"
               :level-as-points="levelsAsPoints"
               :is-edit="isEdit"
                :all-levels="levels"
               @hidden="handleHidden"></new-level>
  </div>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import NewLevel from './NewLevel';
  import SettingService from '../settings/SettingsService';
  import LevelService from './LevelService';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';

  export default {
    name: 'Levels',
    components: {
      SkillsBTable,
      NewLevel,
      SubPageHeader,
    },
    props: {
      maxLevels: {
        type: Number,
        default: 25,
      },
    },
    mixins: [MsgBoxMixin],
    data() {
      return {
        currentlyFocusedLevelId: '',
        displayLevelModal: false,
        isEdit: false,
        levelsAsPoints: false,
        levelToEdit: { iconClass: 'fas fa-user-ninja' },
        levels: [],
        table: {
          options: {
            busy: true,
            bordered: false,
            outlined: true,
            stacked: 'md',
            fields: [
              {
                key: 'level',
                label: 'Level',
                sortable: false,
              },
              {
                key: 'name',
                label: 'Name',
                sortable: false,
              },
              {
                key: 'percent',
                label: 'Percent %',
                sortable: false,
              },
              {
                key: 'points',
                label: 'Points (> to <=)',
                sortable: false,
              },
              {
                key: 'edit',
                label: 'Modify',
                sortable: false,
                headerTitle: 'Edit Level',
              },
            ],
            pagination: {
              remove: true,
            },
          },
        },
      };
    },
    created() {
      SettingService.getSetting(this.$route.params.projectId, 'level.points.enabled')
        .then((data) => {
          if (data) {
            const pointsEnabled = (data.value === true || data.value === 'true');
            if (pointsEnabled) {
              this.levelsAsPoints = true;
              this.levelsColumns = ['iconClass', 'level', 'name', 'pointsFrom', 'pointsTo', 'edit'];
              this.options.headings = {
                level: 'Level',
                name: 'Name',
                pointsFrom: 'From Points (>)',
                pointsTo: 'To Points (<=)',
                edit: '',
                iconClass: '',
              };
            } else {
              this.levelsColumns = ['iconClass', 'level', 'name', 'percent', 'pointsFrom', 'pointsTo', 'edit'];
              this.options.headings = {
                level: 'Level',
                name: 'Name',
                percent: 'Percent',
                pointsFrom: 'From Points (>)',
                pointsTo: 'To Points (<=)',
                edit: '',
                iconClass: '',
              };
            }
          }
        });
    },
    mounted() {
      this.loadLevels();
    },
    computed: {
      bounds() {
        const bounds = {
          previous: null,
          next: null,
        };

        if (this.isEdit) {
          const existingIdx = this.levels.findIndex((level) => this.levelToEdit.level === level.level);
          const byIndex = new Map(this.levels.map((level, index) => [index, level]));

          const previous = byIndex.get(existingIdx - 1);
          const next = byIndex.get(existingIdx + 1);

          if (previous) {
            if (this.levelsAsPoints) {
              bounds.previous = previous.pointsTo;
            } else {
              bounds.previous = previous.percent;
            }
          }
          if (next) {
            if (this.levelsAsPoints) {
              bounds.next = next.pointsFrom;
            } else {
              bounds.next = next.percent;
            }
          }
        } else {
          const last = this.levels[this.levels.length - 1];
          if (this.levelsAsPoints) {
            bounds.previous = last.pointsFrom;
          } else {
            bounds.previous = last.percent;
          }
        }

        return bounds;
      },
      reachedMaxLevels() {
        return this.levels.length >= this.maxLevels;
      },
      onlyOneLevelLeft() {
        return this.levels.length <= 1;
      },
    },
    methods: {
      loadLevels() {
        this.table.options.busy = true;
        if (this.$route.params.subjectId) {
          LevelService.getLevelsForSubject(this.$route.params.projectId, this.$route.params.subjectId)
            .then((response) => {
              this.levels = response;
              this.table.options.busy = false;
            });
        } else {
          LevelService.getLevelsForProject(this.$route.params.projectId)
            .then((response) => {
              this.levels = response;
              this.table.options.busy = false;
            });
        }
        if (this.currentlyFocusedLevelId) {
          this.$nextTick(() => {
            this.handleFocus({ edit: true });
          });
        }
      },
      removeLastItem() {
        if (!this.onlyOneLevelLeft) {
          if (!this.$route.params.subjectId && this.$route.params.projectId) {
            const lastLevel = this.getLastItemLevel();
            LevelService.checkIfProjectLevelBelongsToGlobalBadge(this.$route.params.projectId, lastLevel)
              .then((belongsToGlobalBadge) => {
                if (belongsToGlobalBadge) {
                  this.msgOk(`Cannot remove level: [${lastLevel}].  This project level belongs to one or more global badges. Please contact a Supervisor to remove this dependency.`, 'Unable to delete');
                } else {
                  this.confirmAndRemoveLastItem();
                }
              });
          } else {
            this.confirmAndRemoveLastItem();
          }
        }
      },
      confirmAndRemoveLastItem() {
        const msg = 'Are you absolutely sure you want to delete the highest Level?';
        this.msgConfirm(msg, 'WARNING: Delete Highest Level').then((res) => {
          if (res) {
            this.doRemoveLastItem();
          }
        });
      },
      doRemoveLastItem() {
        this.table.options.busy = true;
        if (this.$route.params.subjectId) {
          LevelService.deleteLastLevelForSubject(this.$route.params.projectId, this.$route.params.subjectId)
            .then(() => {
              this.loadLevels();
            });
        } else {
          LevelService.deleteLastLevelForProject(this.$route.params.projectId)
            .then(() => {
              this.loadLevels();
            });
        }
      },
      getLastItemLevel() {
        const lastLevel = [...this.levels].sort((a, b) => {
          if (a.level > b.level) {
            return 1;
          }
          if (b.level > a.level) {
            return -1;
          }
          return 0;
        })[this.levels.length - 1].level;
        return lastLevel;
      },
      editLevel(existingLevel) {
        this.isEdit = !!existingLevel;

        if (existingLevel) {
          this.levelToEdit = { ...existingLevel };
          if (this.levels[this.levels.length - 1].level === existingLevel.level) {
            this.levelToEdit.isLast = true;
          }
          this.currentlyFocusedLevelId = existingLevel.level;
        } else if (!this.reachedMaxLevels) {
          this.levelToEdit = { iconClass: 'fas fa-user-ninja' };
        }

        this.displayLevelModal = true;
      },
      doCreateNewLevel(nextLevelObj) {
        this.table.options.busy = true;
        if (this.$route.params.subjectId) {
          LevelService.createNewLevelForSubject(this.$route.params.projectId, this.$route.params.subjectId, nextLevelObj)
            .then(() => {
              this.loadLevels();
            });
        } else {
          LevelService.createNewLevelForProject(this.$route.params.projectId, nextLevelObj)
            .then(() => {
              this.loadLevels();
            });
        }
      },
      doEditLevel(editedLevelObj) {
        this.table.options.busy = true;
        if (this.$route.params.subjectId) {
          LevelService.editLevelForSubject(this.$route.params.projectId, this.$route.params.subjectId, editedLevelObj)
            .then(() => {
              this.loadLevels();
            });
        } else {
          LevelService.editLevelForProject(this.$route.params.projectId, editedLevelObj)
            .then(() => {
              this.loadLevels();
            });
        }
      },
      handleHidden(e) {
        if (!e || !e.saved) {
          this.handleFocus(e);
        }
      },
      handleFocus(e) {
        let ref = this.$refs.addLevel;
        if (e && e.edit) {
          const refName = `edit_${this.currentlyFocusedLevelId}`;
          ref = this.$refs[refName];
        }
        this.currentlyFocusedLevelId = '';
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
  #level-def-panel .level-icon {
    font-size: 1.5rem;
    height: 24px;
    width: 24px;
  }

  #level-def-panel .VuePagination__count {
    display: none;
  }

  .icon-warning {
    font-size: 1.5rem;
  }

</style>

<style scoped>

</style>
