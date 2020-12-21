/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
  <div id="shared-skills-table" v-if="this.sharedSkills && this.sharedSkills.length">
    <v-client-table :data="sharedSkills" :columns="columns" :options="options">
      <div slot="edit" slot-scope="props">
        <div v-if="isDeleteEnabled">
          <b-button variant="outline-hc" @click="onDeleteEvent(props.row)">
            <i class="fas fa-trash"/>
          </b-button>
        </div>
      </div>

      <div slot="skill" slot-scope="props">
        <div>{{ props.row.skillName }}</div>
        <div class="text-secondary" style="font-size: 0.9rem;">ID: {{ props.row.skillId }}</div>
      </div>
      <div slot="project" slot-scope="props">
        <div>{{ getProjectName(props.row) }}</div>
        <div class="text-secondary" style="font-size: 0.9rem;">ID: {{ getProjectId(props.row) }}</div>
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
      getProjectName(row) {
        if (row.sharedWithAllProjects) {
          return 'All Projects';
        }
        return row.projectName;
      },
      getProjectId(row) {
        if (row.sharedWithAllProjects) {
          return 'All';
        }
        return row.projectId;
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
