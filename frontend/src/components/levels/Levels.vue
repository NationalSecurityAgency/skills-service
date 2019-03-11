<template>
  <div id="level-def-panel">
    <div class="columns skills-underline-container">
      <div class="column">
        <div class="title">Level Defintions</div>
      </div>
      <div class="column has-text-right">
        <a v-on:click="removeLastItem" class="button is-outlined is-success">
          <span>Remove Highest Level</span>
          <span class="icon is-small">
              <i class="fas fa-trash-alt"/>
            </span>
        </a>

        <a v-on:click="editLevel()" class="button is-outlined is-success">
          <span>Add Next Level</span>
          <span class="icon is-small">
              <i class="fas fa-plus-circle"/>
            </span>
        </a>
      </div>
    </div>

    <div class="skills-bordered-component">
      <b-loading :is-full-page="false" :active.sync="isLoading" :can-cancel="false"></b-loading>
      <v-client-table v-if="levels && levels.length && !isLoading" :data="levels" :columns="levelsColumns"
                      :options="options">
        <span slot="iconClass" slot-scope="props">
          <div class="">
              <i class="has-text-info subject-icon skills-icon level-icon" v-bind:class="`${props.row.iconClass}`"></i>
            </div>
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
          <a v-on:click="editLevel(props.row)" class="button is-outlined is-success">
                    <span class="icon is-small">
                      <i class="fas fa-edit"/>
                    </span>
            <span>Edit</span>
          </a>
        </div>
      </v-client-table>
    </div>
  </div>
</template>

<script>
  import NewLevel from './NewLevel';
  import SettingService from '../settings/SettingsService';
  import LevelService from './LevelService';
  import ToastHelper from '../utils/ToastHelper';

  export default {
    name: 'Levels',
    props: ['projectId', 'subjectId'],
    data() {
      return {
        levelsAsPoints: false,
        isLoading: true,
        serverErrors: [],
        levels: [],
        levelsColumns: ['iconClass', 'level', 'name', 'percent', 'pointsFrom', 'pointsTo', 'edit'],
        options: {
          filterable: false,
          footerHeadings: false,
          headings: {
            level: 'Level',
            name: 'Name',
            percent: 'Percent',
            pointsFrom: 'From Points',
            pointsTo: 'To Points',
            edit: '',
            iconClass: '',
          },
          columnsClasses: {
            edit: 'control-column',
          },
          sortable: [],
          sortIcon: {
            base: 'fa fa-sort', up: 'fa fa-sort-up', down: 'fa fa-sort-down', is: 'fa fa-sort',
          },
          skin: 'table is-striped is-fullwidth',
        },
      };
    },
    created() {
      SettingService.getSetting(this.projectId, 'level.points.enabled').then((data) => {
        if (data) {
          const pointsEnabled = (data.value === true || data.value === 'true');
          if (pointsEnabled) {
            this.levelsAsPoints = true;
            this.levelsColumns = ['iconClass', 'level', 'name', 'pointsFrom', 'pointsTo', 'edit'];
            this.options.headings = {
              level: 'Level',
              name: 'Name',
              pointsFrom: 'From Points',
              pointsTo: 'To Points',
              edit: '',
              iconClass: '',
            };
          } else {
            this.levelsColumns = ['iconClass', 'level', 'name', 'percent', 'pointsFrom', 'pointsTo', 'edit'];
            this.options.headings = {
              level: 'Level',
              name: 'Name',
              percent: 'Percent',
              pointsFrom: 'From Points',
              pointsTo: 'To Points',
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
    methods: {
      loadLevels() {
        if (this.subjectId) {
          LevelService.getLevelsForSubject(this.projectId, this.subjectId).then((response) => {
            this.isLoading = false;
            this.levels = response;
          })
            .catch((e) => {
              this.serverErrors.push(e);
          });
        } else {
          LevelService.getLevelsForProject(this.projectId).then((response) => {
            this.isLoading = false;
            this.levels = response;
          })
            .catch((e) => {
              this.serverErrors.push(e);
          });
        }
      },
      removeLastItem() {
        if (this.levels.length > 1) {
          this.$dialog.confirm({
            title: 'WARNING: Delete Highest Level',
            message: 'Are you absolutely sure you want to delete the highest Level?',
            confirmText: 'Delete',
            type: 'is-danger',
            hasIcon: true,
            icon: 'exclamation-triangle',
            iconPack: 'fa',
            scroll: 'keep',
            onConfirm: () => this.doRemoveLastItem(),
          });
        } else {
          this.$toast.open(ToastHelper.defaultConf('You must retain at least one level'));
        }
      },
      doRemoveLastItem() {
        this.isLoading = true;
        if (this.subjectId) {
          LevelService.deleteLastLevelForSubject(this.projectId, this.subjectId).then(() => {
            this.isLoading = false;
            this.loadLevels();
          })
            .catch((e) => {
              this.serverErrors.push(e);
              this.showError('Error removing level', e);
          });
        } else {
          LevelService.deleteLastLevelForProject(this.projectId).then(() => {
            this.isLoading = false;
            this.loadLevels();
          })
            .catch((e) => {
              this.serverErrors.push(e);
              this.showError('Error removing level', e);
          });
        }
      },
      editLevel(existingLevel) {
        let editProps = null;

        if (existingLevel) {
          const bounds = {
            previous: null,
            next: null,
          };

          const existingIdx = this.levels.findIndex(level => existingLevel.level === level.level);
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

          editProps = {
            isEdit: true,
            percent: existingLevel.percent,
            pointsFrom: existingLevel.pointsFrom,
            pointsTo: existingLevel.pointsTo,
            name: existingLevel.name,
            iconClass: existingLevel.iconClass || 'fas fa-user-ninja',
            level: existingLevel.level,
            levelId: existingLevel.id,
            boundaries: bounds,
          };
        } else {
          const last = this.levels[this.levels.length - 1];
          const bounds = {
            previous: null,
            next: null,
          };

          if (this.levelsAsPoints) {
            bounds.previous = last.pointsFrom;
          } else {
            bounds.previous = last.percent;
          }

          editProps = {
            levelAsPoints: this.levelsAsPoints,
            iconClass: 'fas fa-user-ninja',
            isEdit: false,
            boundaries: bounds,
          };
        }

        this.$modal.open({
          parent: this,
          component: NewLevel,
          hasModalCard: true,
          props: editProps,
          events: {
            'new-level': this.doCreateNewLevel,
            'edited-level': this.doEditLevel,
          },
        });
      },
      doCreateNewLevel(nextLevelObj) {
        this.loading = true;
        if (this.subjectId) {
          LevelService.createNewLevelForSubject(this.projectId, this.subjectId, nextLevelObj).then(() => {
            this.isLoading = false;
            this.loadLevels();
          })
            .catch((e) => {
              this.serverErrors.push(e);
              this.showError('Error creating level', e);
          });
        } else {
          LevelService.createNewLevelForProject(this.projectId, nextLevelObj).then(() => {
            this.isLoading = false;
            this.loadLevels();
          })
            .catch((e) => {
              this.serverErrors.push(e);
              this.showError('Error creating level', e);
          });
        }
      },
      doEditLevel(editedLevelObj) {
        this.loading = true;
        if (this.subjectId) {
          LevelService.editlevelForSubject(this.projectId, this.subjectId, editedLevelObj).then(() => {
            this.isLoading = false;
            this.loadLevels();
          })
            .catch((e) => {
              this.serverErrors.push(e);
              this.showError('Error editing level', e);
          });
        } else {
          LevelService.editlevelForProject(this.projectId, editedLevelObj).then(() => {
            this.isLoading = false;
            this.loadLevels();
          })
            .catch((e) => {
              this.serverErrors.push(e);
              this.showError('Error editing level', e);
          });
        }
      },
      showError(msgPrefix, e) {
        if (e && e.response) {
          if (e.response.data && e.response.data.errorMsg) {
            this.$toast.open(ToastHelper.defaultConf(`${msgPrefix}: ${e.response.data.errorMsg}`, true));
          } else {
            this.$toast.open(ToastHelper.defaultConf(`${msgPrefix}: ${e.response.status} - ${e.response.statusText}`, true));
          }
        }
      },
    },
  };
</script>

<style>
  #level-def-panel .control-column{
    width: 4rem;
    /*background: yellow;*/
  }

  #level-def-panel .level-icon {
    font-size: 1.5rem;
    height: 24px;
    width: 24px;
  }

  #level-def-panel .VuePagination__count {
    display: none;
  }


</style>

<style scoped>

</style>

