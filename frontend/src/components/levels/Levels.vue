<template>
  <div id="level-def-panel">
    <sub-page-header title="Level Definitions">
      <div class="row">
        <div class="col">

          <b-tooltip target="remove-button" title="You must retain at least one level." :disabled="!onlyOneLevelLeft"></b-tooltip>
          <span id="remove-button" class="mr-2">
            <b-button variant="outline-primary" @click="removeLastItem" :disabled="onlyOneLevelLeft">
              <span class="d-none d-sm-inline">Remove</span> Highest <i class="fas fa-trash-alt"/>
            </b-button>
          </span>
          <b-tooltip target="add-button" title="Reached maximum limit of levels." :disabled="!reachedMaxLevels"></b-tooltip>
          <span id="add-button">
            <b-button @click="editLevel()" variant="outline-primary" :disabled="reachedMaxLevels">
              <span class="d-none d-sm-inline">Add</span> Next <i class="fas fa-plus-circle" />
            </b-button>
          </span>
        </div>
      </div>
    </sub-page-header>


    <loading-container :is-loading="isLoading">
      <simple-card>
        <v-client-table v-if="levels && levels.length && !isLoading" :data="levels" :columns="levelsColumns"
                        :options="options">
        <span slot="iconClass" slot-scope="props">
              <i class="text-info level-icon" v-bind:class="`${props.row.iconClass}`"></i>
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
            <b-button @click="editLevel(props.row)" variant="outline-info" style="width: 5rem;">
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
               :is-edit="isEdit"></new-level>
  </div>
</template>

<script>
  import NewLevel from './NewLevel';
  import SettingService from '../settings/SettingsService';
  import LevelService from './LevelService';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import LoadingContainer from '../utils/LoadingContainer';
  import SimpleCard from '../utils/cards/SimpleCard';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';

  export default {
    name: 'Levels',
    components: {
      NewLevel,
      SimpleCard,
      LoadingContainer,
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
        displayLevelModal: false,
        isEdit: false,
        levelsAsPoints: false,
        isLoading: true,
        levelToEdit: { iconClass: 'fas fa-user-ninja' },
        levels: [],
        levelsColumns: ['iconClass', 'level', 'name', 'percent', 'pointsFrom', 'pointsTo', 'edit'],
        options: {
          filterable: false,
          footerHeadings: false,
          headings: {
            level: 'Level',
            name: 'Name',
            percent: 'Percent %',
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
    computed: {
      bounds() {
        const bounds = {
          previous: null,
          next: null,
        };

        if (this.isEdit) {
          const existingIdx = this.levels.findIndex(level => this.levelToEdit.level === level.level);
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
        if (this.$route.params.subjectId) {
          LevelService.getLevelsForSubject(this.$route.params.projectId, this.$route.params.subjectId)
            .then((response) => {
              this.isLoading = false;
              this.levels = response;
            });
        } else {
          LevelService.getLevelsForProject(this.$route.params.projectId)
            .then((response) => {
              this.isLoading = false;
              this.levels = response;
            });
        }
      },
      removeLastItem() {
        if (!this.onlyOneLevelLeft) {
          const msg = 'Are you absolutely sure you want to delete the highest Level?';
          this.msgConfirm(msg, 'WARNING: Delete Highest Level').then((res) => {
            if (res) {
              this.doRemoveLastItem();
            }
          });
        }
      },
      doRemoveLastItem() {
        this.isLoading = true;
        if (this.$route.params.subjectId) {
          LevelService.deleteLastLevelForSubject(this.$route.params.projectId, this.$route.params.subjectId)
            .then(() => {
              this.isLoading = false;
              this.loadLevels();
            });
        } else {
          LevelService.deleteLastLevelForProject(this.$route.params.projectId)
            .then(() => {
              this.isLoading = false;
              this.loadLevels();
            });
        }
      },
      editLevel(existingLevel) {
        this.isEdit = !!existingLevel;

        if (existingLevel) {
          this.levelToEdit = Object.assign({}, existingLevel);
          if (this.levels[this.levels.length - 1].level === existingLevel.level) {
            this.levelToEdit.isLast = true;
          }
        } else if (!this.reachedMaxLevels) {
          this.levelToEdit = { iconClass: 'fas fa-user-ninja' };
        }

        this.displayLevelModal = true;
      },
      doCreateNewLevel(nextLevelObj) {
        this.loading = true;
        if (this.$route.params.subjectId) {
          LevelService.createNewLevelForSubject(this.$route.params.projectId, this.$route.params.subjectId, nextLevelObj)
            .then(() => {
              this.isLoading = false;
              this.loadLevels();
            });
        } else {
          LevelService.createNewLevelForProject(this.$route.params.projectId, nextLevelObj)
            .then(() => {
              this.isLoading = false;
              this.loadLevels();
            });
        }
      },
      doEditLevel(editedLevelObj) {
        this.loading = true;
        if (this.$route.params.subjectId) {
          LevelService.editLevelForSubject(this.$route.params.projectId, this.$route.params.subjectId, editedLevelObj)
            .then(() => {
              this.isLoading = false;
              this.loadLevels();
            });
        } else {
          LevelService.editLevelForProject(this.$route.params.projectId, editedLevelObj)
            .then(() => {
              this.isLoading = false;
              this.loadLevels();
            });
        }
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
