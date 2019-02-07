<template>
    <div id="shared-skills-table" v-if="this.sharedSkills && this.sharedSkills.length">
      <v-client-table :data="sharedSkills" :columns="columns" :options="options">
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
          </span>
          </div>
        </div>

        <div slot="skill" slot-scope="props" class="field has-addons">
          <div class="columns">
            <div class="column">
              <div>{{ props.row.skillName }}</div>
              <div class="subtitle has-text-grey" style="font-size: 0.9rem;">ID: {{ props.row.skillId }}</div>
            </div>
          </div>
        </div>
        <div slot="project" slot-scope="props" class="field has-addons">
          <div class="columns">
            <div class="column">
              <div>{{ props.row.projectName }}</div>
              <div class="subtitle has-text-grey" style="font-size: 0.9rem;">ID: {{ props.row.projectId }}</div>
            </div>
          </div>
        </div>

      </v-client-table>
    </div>
</template>

<script>
  export default {
    name: 'SharedSkillsTable',
    props: ['sharedSkills'],
    data() {
      return {
        columns: ['skill', 'project', 'edit'],
        options: {
          headings: {
            skill: 'Shared Skill',
            project: 'Share to Project',
            edit: '',
          },
          perPage: 15,
          columnsClasses: {
            edit: 'control-column',
          },
          pagination: { dropdown: false, edge: false },
          sortable: ['skillName', 'projectName'],
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
  #shared-skills-table .VueTables__limit-field {
    display:none;
  }

  #shared-skills-table .VuePagination__count {
    display: none;
  }

  #shared-skills-table .control-column{
    width: 3rem;
  }

</style>
