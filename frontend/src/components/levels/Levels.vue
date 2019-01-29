<template>
  <div>
    <div class="columns">
      <div class="column">
        <div class="title">Level Defintions</div>
      </div>
      <div class="column has-text-right">
        <a v-on:click="removeLastItem" class="button is-outlined is-info">
          <span>Remove Highest Level</span>
          <span class="icon is-small">
              <i class="fas fa-trash-alt"/>
            </span>
        </a>

        <a v-on:click="createNevLevel" class="button is-outlined is-info">
          <span>Add Next Level</span>
          <span class="icon is-small">
              <i class="fas fa-plus-circle"/>
            </span>
        </a>
      </div>
    </div>

    <b-loading :is-full-page="false" :active.sync="isLoading" :can-cancel="false"></b-loading>
    <v-client-table v-if="levels && levels.length && !isLoading" :data="levels" :columns="levelsColumns"
                    :options="options">
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
        <a class="button is-outlined is-info">
                  <span class="icon is-small">
                    <i class="fas fa-edit"/>
                  </span>
          <span>Edit</span>
        </a>
      </div>
    </v-client-table>
  </div>
</template>

<script>
  import axios from 'axios';
  import NewLevel from './NewLevel';

  export default {
    name: 'Levels',
    props: ['projectId', 'subjectId'],
    data() {
      return {
        isLoading: true,
        serverErrors: [],
        levels: [],
        levelsColumns: ['level', 'percent', 'pointsFrom', 'pointsTo', 'edit'],
        options: {
          filterable: false,
          footerHeadings: false,
          headings: {
            level: 'Level',
            percent: 'Percent',
            pointsFrom: 'From Points',
            pointsTo: 'To Points',
            edit: '',
          },
          columnsClasses: {
            edit: 'control-column',
          },
          sortable: ['skillId', 'name', 'pointIncrement', 'totalPoints'],
          sortIcon: {
            base: 'fa fa-sort', up: 'fa fa-sort-up', down: 'fa fa-sort-down', is: 'fa fa-sort',
          },
          skin: 'table is-striped is-fullwidth',
        },
      };
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
      createNevLevel() {
        this.$modal.open({
          parent: this,
          component: NewLevel,
          hasModalCard: true,
          props: {
            project: { name: '', projectId: '' },
          },
          events: {
            'new-level': this.doCreateNewLevel,
          },
        });
      },
      doCreateNewLevel(levelPercent) {
        this.loading = true;
        const url = `${this.getLevelsUrl()}/next`;
        axios.put(url, {
          percent: levelPercent,
        })
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
  .control-column{
    width: 4rem;
    /*background: yellow;*/
  }
</style>

<style scoped>

</style>
