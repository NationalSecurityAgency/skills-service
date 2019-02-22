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
  import axios from 'axios';
  import NewLevel from './NewLevel';
  import SettingService from '../settings/SettingsService';

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
      getLevelsUrl() {
        let url = `/admin/projects/${this.projectId}`;
        if (this.subjectId) {
          url = `${url}/subjects/${this.subjectId}/levels`;
        } else {
          url = `${url}/levels`;
        }
        return url;
      },
      loadLevels() {
        const url = this.getLevelsUrl();
        axios.get(url)
          .then((response) => {
            this.isLoading = false;
            this.levels = response.data;
          })
          .catch((e) => {
            this.serverErrors.push(e);
        });
      },
      removeLastItem() {
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
      },
      doRemoveLastItem() {
        this.isLoading = true;
        const url = `${this.getLevelsUrl()}/last`;
        axios.delete(url)
          .then(() => {
            this.loadLevels();
          })
          .catch((e) => {
            this.serverErrors.push(e);
        });
      },
      editLevel(existingLevel) {
        let editProps = null;

        if (existingLevel) {
          editProps = {
            isEdit: true,
            percent: existingLevel.percent,
            pointsFrom: existingLevel.pointsFrom,
            pointsTo: existingLevel.pointsTo,
            name: existingLevel.name,
            iconClass: existingLevel.iconClass,
            level: existingLevel.level,
            levelId: existingLevel.id,
          };
        } else {
          editProps = {
            levelAsPoints: this.levelsAsPoints,
            iconClass: 'fas fa-arrow-circle-up',
            isEdit: false,
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
        const url = `${this.getLevelsUrl()}/next`;
        axios.put(url, nextLevelObj)
          .then(() => {
            this.loadLevels();
          })
          .catch((e) => {
            this.serverErrors.push(e);
        });
      },
      doEditLevel(editedLevelObj) {
        this.loading = true;
        const url = `${this.getLevelsUrl()}/edit/${editedLevelObj.id}`;
        axios.put(url, editedLevelObj)
          .then(() => {
            this.loadLevels();
          })
          .catch((e) => {
            this.serverErrors.push(e);
        });
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

