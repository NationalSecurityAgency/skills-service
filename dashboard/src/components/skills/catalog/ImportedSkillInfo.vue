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
  <div :data-cy="`importSkillInfo-${skill.projectId}_${skill.skillId}`"  class="ml-5">

    <div v-if="skill.importedProjectCount > 0">
      <skills-b-table :options="table.options"
                      :items="importedProjects"
                      tableStoredStateId="importedSkillsTable"
                      data-cy="importedSkillsTable">
        <template #head(importingProjectName)="data">
          <span class="text-primary"><i
            class="fas fa-graduation-cap skills-color-skills"/> {{ data.label }}</span>
        </template>
        <template #head(importedOn)="data">
          <span class="text-primary"><i
            class="fas fa-clock skills-color-projects"></i> {{ data.label }}</span>
        </template>

        <template v-slot:cell(importingProjectName)="data">
        <div class="row">
          <div class="col">
            <span class="ml-2">{{ data.value }}</span>
            <span v-if="data.item.enabled !== 'true'" class="text-uppercase ml-2"><b-badge variant="warning">Disabled</b-badge></span>
          </div>
          <div class="col-auto">
            <b-button variant="outline-primary" :aria-label="`Contact ${data.item.name} project owner`"
                             @click="chooseProject(data.value, skill.projectId)" :data-cy="`contactOwnerBtn_${ skill.projectId }`">
                    Contact <i aria-hidden="true" class="fas fas fa-mail-bulk"/>
             </b-button>
          </div>
          </div>
        </template>

        <template v-slot:cell(importedOn)="data">
          <date-cell :value="data.value" />
        </template>
      </skills-b-table>
        <div>
          <contact-owners-dialog v-if="contactModal.show"  :projectName="contactModal.projectName"
                                   v-model="contactModal.show" :projectId="contactModal.projectId" /></div>
    </div>
    <div v-else>
      <div class="h6">This skill has not been imported by any other projects yet...</div>
    </div>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import CatalogService from '@/components/skills/catalog/CatalogService';
  import DateCell from '@/components/utils/table/DateCell';
  import ContactOwnersDialog from '@/components/myProgress/ContactOwnersDialog';

  export default {
    name: 'ImportedSkillInfo',
    components: {
      SkillsBTable,
      DateCell,
      ContactOwnersDialog,
    },
    props: {
      skill: Object,
    },
    data() {
      return {
        importedProjects: [],
        contactModal: {
          show: false,
          projectId: null,
          projectName: null,
        },
        table: {
          options: {
            sortBy: 'exportedOn',
            sortDesc: true,
            busy: true,
            stacked: 'md',
            bordered: true,
            outlined: true,
            tableDescription: 'Imported Skill Details',
            pagination: {
              remove: true,
            },
            fields: [
              {
                key: 'importingProjectName',
                label: 'Importing Project',
                sortable: true,
                sortKey: 'importingProjectName',
              }, {
                key: 'importedOn',
                label: 'Imported On',
                sortable: true,
                sortKey: 'importedOn',
              },
            ],
          },
        },
      };
    },
    mounted() {
      this.loadImportedProjectDetails(this.skill);
    },
    computed: {
      ...mapGetters([
         'isEmailEnabled',
      ]),
    },
    methods: {
      loadImportedProjectDetails(skill) {
        if (skill.importedProjectCount > 0) {
          this.table.options.busy = true;
          CatalogService.getExportedStats(skill.projectId, skill.skillId)
            .then((res) => {
              this.importedProjects = res.users;
            }).finally(() => {
              this.table.options.busy = false;
            });
        }
      },
      showContactOwner() {
        this.contactModal.show = true;
      },
      chooseProject(name, id) {
        this.contactModal.projectName = name;
        this.contactModal.projectId = id;
        this.showContactOwner();
      },
    },
  };
</script>

<style scoped>

</style>
