<template>
    <div id="simple-skills-table" v-if="this.skills && this.skills.length">
      <v-client-table :data="skills" :columns="columns" :options="options">
        <div slot="edit" slot-scope="props">
          <div class="field has-addons has-text-right">
          <span class="field has-addons">
            <p class="">
              <a v-on:click="onDeleteEvent(props.row)" class="button is-outlined is-info">
                    <span class="icon is-small">
                      <i class="fas fa-trash"/>
                    </span>
              </a>
            </p>
            <p class="skills-pad-left-1-rem">
              <router-link :to="{ name:'SkillPage',
              params: { projectId: props.row.projectId, subjectId: props.row.subjectId, skillId: props.row.skillId }}"
                           class="button is-outlined is-info"  v-bind:class="{ active: !props.row.subjectId }">
                <span>Manage</span>
                <span class="icon is-small">
                  <i class="fas fa-arrow-circle-right"/>
                </span>
              </router-link>
            </p>
          </span>
          </div>
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
        columns: ['name', 'skillId', 'pointIncrement', 'totalPoints', 'edit'],
        options: {
          headings: {
            name: 'Skill Name',
            skillId: 'Skill ID',
            pointIncrement: 'Point Increment',
            totalPoints: 'Total Points',
            edit: '',
          },
          perPage: 15,
          columnsClasses: {
            edit: 'control-column',
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
    display:none;
  }

  #simple-skills-table .control-column{
    width: 11rem;
    /*background: yellow;*/
  }

  #simple-skills-table .active {
    cursor: not-allowed;
    pointer-events: none;
    color: #c0c0c0;
    background-color: #ffffff;
    border-color: gray;
  }

</style>
