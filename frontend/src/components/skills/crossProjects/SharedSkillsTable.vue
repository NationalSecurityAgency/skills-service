<template>
  <div id="shared-skills-table" v-if="this.sharedSkills && this.sharedSkills.length">
    <v-client-table :data="sharedSkills" :columns="columns" :options="options">
      <div slot="edit" slot-scope="props">
        <div v-if="isDeleteEnabled">
          <b-button variant="outline-primary" @click="onDeleteEvent(props.row)">
            <i class="fas fa-trash"/>
          </b-button>
        </div>
      </div>

      <div slot="skill" slot-scope="props">
        <div>{{ props.row.skillName }}</div>
        <div class="text-secondary" style="font-size: 0.9rem;">ID: {{ props.row.skillId }}</div>
      </div>
      <div slot="project" slot-scope="props">
        <div>{{ props.row.projectName }}</div>
        <div class="text-secondary" style="font-size: 0.9rem;">ID: {{ props.row.projectId }}</div>
      </div>

    </v-client-table>
  </div>
</template>

<script>
  export default {
    name: 'SharedSkillsTable',
    props: ['sharedSkills', 'disableDelete'],
    data() {
      return {
        columns: ['skill', 'project', 'edit'],
        options: {
          headings: {
            skill: 'Shared Skill',
            project: 'Project',
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
        isDeleteEnabled: true,
      };
    },
    mounted() {
      if (this.disableDelete) {
        this.isDeleteEnabled = false;
      }
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
    display: none;
  }

  #shared-skills-table .VuePagination__count {
    display: none;
  }

  #shared-skills-table .control-column {
    width: 3rem;
  }

</style>
