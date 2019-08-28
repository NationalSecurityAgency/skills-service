<template>
  <div id="simple-skills-table" v-if="this.skills && this.skills.length">
    <v-client-table :data="skills" :columns="columns" :options="options">
      <div slot="edit" slot-scope="props">
        <div class="field text-right">
          <span class="field">
              <button v-on:click="onDeleteEvent(props.row)" class="btn btn-sm btn-outline-primary">
                      <i class="fas fa-trash"/>
              </button>
                <router-link v-if="props.row.subjectId" :id="props.row.skillId" :to="{ name:'SkillOverview',
                params: { projectId: props.row.projectId, subjectId: props.row.subjectId, skillId: props.row.skillId }}"
                             class="btn btn-sm btn-outline-primary ml-2">
                  Manage <i class="fas fa-arrow-circle-right"/>
                </router-link>
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
    name: 'SimpleSkillsTable',
    props: ['skills'],
    data() {
      return {
        columns: ['name', 'skillId', 'totalPoints', 'edit'],
        options: {
          headings: {
            name: 'Skill Name',
            skillId: 'Skill ID',
            totalPoints: 'Total Points',
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
          sortable: ['name', 'skillId', 'pointIncrement', 'totalPoints'],
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
      onDeleteEvent(skill) {
        this.$emit('skill-removed', skill);
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
