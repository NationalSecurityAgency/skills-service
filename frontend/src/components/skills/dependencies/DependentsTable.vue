<template>
    <div id="dependents-table" v-if="this.skills && this.skills.length">
      <v-client-table :data="skills" :columns="columns" :options="options">
        <div slot="edit" slot-scope="props">
          <div class="field has-addons">
          <span class="field has-addons">
            <p class="">
              <a v-on:click="onDeleteEvent(props.row)" class="button is-outlined is-info">
                    <span class="icon is-small">
                      <i class="fas fa-trash"/>
                    </span>
                <!--<span>Remove</span>-->
              </a>
            </p>
            <p class="skills-pad-left-1-rem">
              <router-link :to="{ name:'SkillPage',
              params: { projectId: props.row.projectId, subjectId: props.row.subjectId, skillId: props.row.skillId }}"
                           class="button is-outlined is-info">
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
    name: 'DependentsTable',
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
          perPage: 5,
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
  #dependents-table .VueTables__limit-field {
    display:none;
  }

  #dependents-table .control-column{
    width: 11rem;
    /*background: yellow;*/
  }

</style>
