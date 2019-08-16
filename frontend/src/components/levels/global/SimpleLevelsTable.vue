<template>
  <div id="simple-levels-table" v-if="this.levels && this.levels">
    <v-client-table :data="levels" :columns="columns" :options="options">
      <div slot="edit" slot-scope="props">
        <div class="field text-right">
          <span class="field">
              <button v-on:click="onDeleteEvent(props.row)" class="btn btn-sm btn-outline-primary">
                      <i class="fas fa-trash"/>
              </button>
          </span>
        </div>
      </div>
      <div slot="name" slot-scope="props">
        <!-- allow to override how name field is rendered-->
        <slot name="name-cell" v-bind:props="props.row">
          {{ props.row.name }}
        </slot>
      </div>
    </v-client-table>
  </div>
</template>

<script>
  export default {
    name: 'SimpleLevelsTable',
    props: ['levels'],
    data() {
      return {
        columns: ['projectName', 'level', 'edit'],
        options: {
          headings: {
            name: 'Project Name',
            skillId: 'Level',
            edit: '',
          },
          perPage: 15,
          columnsClasses: {
            edit: 'control-column',
          },
          columnsDisplay: {
            skillId: 'not_mobile',
            pointIncrement: 'not_mobile',
            totalPoints: 'not_mobile',
          },
          pagination: { dropdown: false, edge: false },
          sortable: ['projectName', 'level'],
          sortIcon: {
            base: 'fa fa-sort', up: 'fa fa-sort-up', down: 'fa fa-sort-down', is: 'fa fa-sort',
          },
          // highlightMatches: true,
          skin: 'table is-striped is-fullwidth',
          filterable: false,
        },
      };
    },
    methods: {
      onDeleteEvent(level) {
        this.$emit('level-removed', level);
      },
    },
  };
</script>

<style>
  #simple-skills-table .VueTables__limit-field {
    display: none;
  }

  #simple-skills-table .control-column {
    width: 10rem;
  }

  /* on the mobile platform some of the columns will be removed
     so let's allow the table to size on its own*/
  @media (max-width: 576px) {
    #simple-skills-table .control-column {
      width: unset;
    }
  }


  #simple-skills-table .notactive {
    cursor: not-allowed;
    pointer-events: none;
    color: #c0c0c0;
    background-color: #ffffff;
    border-color: gray;
  }

</style>
