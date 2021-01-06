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
  <div id="shared-skills-table" v-if="sharedSkills && sharedSkills.length">
    <skills-b-table v-if="loaded" :options="table.options" :items="sharedSkills" data-cy="sharedSkillsTable">
      <template v-slot:cell(skillName)="data">
        <div>{{ data.value }}</div>
        <div class="text-secondary" style="font-size: 0.9rem;">ID: {{ data.item.skillId }}</div>
      </template>

      <template v-slot:cell(projectName)="data">
        <div><i v-if="data.item.sharedWithAllProjects" class="fas fa-globe text-secondary" /> {{ getProjectName(data.item) }}</div>
        <div v-if="data.item.projectName" class="text-secondary" style="font-size: 0.9rem;">ID: {{ getProjectId(data.item) }}</div>
      </template>
      <template v-slot:cell(edit)="data">
            <b-button @click="onDeleteEvent(data.item)"
                      variant="outline-info" size="sm" class="text-info"
                      :aria-label="`Remove shared skill ${data.item.skillName}`"
                      data-cy="sharedSkillsTable-removeBtn"><i class="fa fa-trash"/></b-button>
      </template>
    </skills-b-table>
  </div>
</template>

<script>
  import SkillsBTable from '../../utils/table/SkillsBTable';

  export default {
    name: 'SharedSkillsTable',
    components: { SkillsBTable },
    props: ['sharedSkills', 'disableDelete'],
    data() {
      return {
        loaded: false,
        table: {
          options: {
            busy: false,
            bordered: false,
            outlined: true,
            stacked: 'md',
            fields: [
              {
                key: 'skillName',
                label: 'Shared Skill',
                sortable: true,
              },
              {
                key: 'projectName',
                label: 'Project',
                sortable: true,
              },
            ],
            pagination: {
              remove: true,
            },
          },
        },
      };
    },
    mounted() {
      if (!this.disableDelete) {
        this.table.options.fields.push({
          key: 'edit',
          label: 'Remove',
          sortable: false,
        });
      }
      this.loaded = true;
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

<style scoped>

</style>
