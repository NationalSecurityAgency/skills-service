<template>
  <div>
    <div class="columns">
      <div class="column">
        <div class="title">Levels</div>
      </div>
      <div class="column has-text-right">
        <a v-on:click="removeLevel" class="button is-outlined is-info">
          <span>Remove Level</span>
          <span class="icon is-small">
              <i class="fas fa-trash-alt"/>
            </span>
        </a>

        <a v-on:click="newLevel" class="button is-outlined is-info">
          <span>Add Level</span>
          <span class="icon is-small">
              <i class="fas fa-plus-circle"/>
            </span>
        </a>
      </div>
    </div>

    <v-client-table :data="levels" :columns="levelsColumns" :options="options"></v-client-table>
  </div>
</template>

<script>
  import ToastHelper from '../utils/ToastHelper';

  export default {
    name: 'Levels',
    data() {
      return {
        isLoading: true,
        errors: [],
        levels: [
          {
            level: 1, percent: 10, fromPts: 100, toPts: 500,
          },
        ],
        levelsColumns: ['level', 'percent', 'fromPts', 'toPts'],
        options: {
          filterable: false,
          footerHeadings: false,
          headings: {
            skillId: 'Skill Id',
            name: 'Skill Name',
            pointIncrement: 'Point Increment',
            totalPoints: 'Total Points',
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
    methods: {
      removeLevel(event) {
        this.$toast.open(ToastHelper.defaultConf(`remove level placeholder ${event}`));
      },
      newLevel() {
        this.$toast.open(ToastHelper.defaultConf('new level placeholder'));
      },
    },
  };
</script>

<style scoped>

</style>
