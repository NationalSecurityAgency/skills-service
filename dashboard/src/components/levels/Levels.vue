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
            <b-button variant="outline-primary" ref="removeNextLevel" @click="removeLastItem" :disabled="onlyOneLevelLeft" size="sm"
                      data-cy="removeLevel">
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

    <b-card body-class="p-0">
      <skills-spinner :is-loading="loading" />
      <skills-b-table v-if="!loading" :options="table.options" :items="levels" data-cy="levelsTable">
        <template v-slot:cell(level)="data">
          {{ data.value }}
          <i v-if="data.item.achievable === false" class="icon-warning fa fa-exclamation-circle text-warning"
             v-b-tooltip.hover="'Level is unachievable. Insufficient available points in project.'"/>
        </template>

        <template v-slot:cell(name)="data">
          <i :class="data.item.iconClass" class="level-icon text-info mr-2" />
          <span data-cy="levelsTable_name">{{ data.value }}</span>
        </template>

        <template v-slot:cell(points)="data">
          <span v-if="data.item.pointsFrom !== null && data.item.pointsFrom !== undefined">
            <span>
              {{ data.item.pointsFrom | number }}
            </span>
            <span class="text-muted">
              to
            </span>
            <span v-if="data.item.pointsTo">{{data.item.pointsTo | number}}</span>
            <span v-else><i class="fas fa-infinity"/></span>
          </span>
          <span v-else>N/A <span class="text-muted small"><i class="fa fa-exclamation-circle"/> Please create more rules first</span></span>
        </template>

        <template #cell(edit)="data">
          <b-button :ref="`edit_${data.item.level}`" @click="editLevel(data.item)" variant="outline-info" size="sm"
                     data-cy="editLevelButton">
            <i class="fas fa-edit"/> Edit
          </b-button>
        </template>

      </skills-b-table>
    </b-card>
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
  import SkillsSpinner from '@/components/utils/SkillsSpinner';

  import NewLevel from './NewLevel';
  import SettingService from '../settings/SettingsService';
  import LevelService from './LevelService';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';

  export default {
    name: 'Levels',
    components: {
      SkillsSpinner,
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
        loading: true,
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
            fields: [],
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
          const fields = [
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
          ];
          this.table.options.fields = fields;

          const pointsEnabled = data && (data.value === true || data.value === 'true');
          if (pointsEnabled) {
            this.levelsAsPoints = true;
            this.table.options.fields = fields.filter((item) => item.key !== 'percent');
          }
        }).finally(() => {
          this.loading = false;
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
              this.handleFocusOnNextTick();
            });
        } else {
          LevelService.getLevelsForProject(this.$route.params.projectId)
            .then((response) => {
              this.levels = response;
              this.table.options.busy = false;
              this.handleFocusOnNextTick();
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
      handleFocusOnNextTick() {
        if (this.currentlyFocusedLevelId) {
          this.$nextTick(() => {
            this.handleFocus({ edit: true });
          });
        }
      },
      handleFocus(e) {
        let ref = this.$refs.addLevel;
        if (e && e.edit) {
          const refName = `edit_${this.currentlyFocusedLevelId}`;
          ref = this.$refs[refName];
          console.log(`Focusing on ${refName}, ref is ${JSON.stringify(ref)}`);
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
