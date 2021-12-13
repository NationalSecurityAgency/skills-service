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
/*
Copyright 2021 SkillTree

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
  <div id="exportedSkillsPanel">
    <b-card body-class="p-0">
      <template #header>
        <div class="h6 mb-0 font-weight-bold">Exported to Catalog</div>
      </template>

      <skills-b-table :options="table.options"
                      :items="exportedSkills"
                      data-cy="exportedSkillsTable"
                      @page-changed="pageChanged"
                      @page-size-changed="pageSizeChanged"
                      @sort-changed="sortTable">
        <template #head(skillName)="data">
          <span class="text-primary"><i
            class="fas fa-graduation-cap skills-color-skills"/> {{ data.label }}</span>
        </template>
        <template #head(subjectName)="data">
          <span class="text-primary"><i
            class="fas fa-cubes skills-color-subjects"></i> {{ data.label }}</span>
        </template>
        <template #head(exportedOn)="data">
          <span class="text-primary"><i
            class="fas fa-clock skills-color-projects"></i> {{ data.label }}</span>
        </template>

        <template v-slot:cell(skillName)="data">
          <div class="row" :data-cy="`nameCell_${data.item.skillId}`">
            <div class="col">
              <div>
                <router-link :data-cy="`viewSkillLink_${data.item.skillId}`" tag="a" :to="{ name:'SkillOverview',
                                        params: { projectId: data.item.projectId, subjectId: data.item.subjectId, skillId: data.item.skillId }}"
                             :aria-label="`View skill ${data.item.skillName} via link`">
                  <div class="h5 d-inline-block">{{ data.item.skillName }}</div>
                </router-link>
              </div>
              <div class="text-secondary sub-info">
                <span>ID:</span> {{ data.item.skillId }}
              </div>
            </div>
            <div class="col-auto ml-auto mr-0">
              <b-button-group size="sm" class="ml-1">
                <b-button :id="`deleteSkillButton_${data.item.skillId}`"
                          @click="removeExported(data.item)" variant="outline-primary"
                          :data-cy="`deleteSkillButton_${data.item.skillId}`"
                          :aria-label="'delete Skill '+data.item.name"
                          title="Delete Skill"
                          size="sm">
                  <i class="text-warning fas fa-trash" aria-hidden="true"/>
                </b-button>
              </b-button-group>
            </div>
          </div>
        </template>
        <template v-slot:cell(subjectName)="data">
          <div class="h5 d-inline-block">{{ data.item.subjectName }}</div>
          <div class="text-secondary sub-info">
            <span>ID:</span> {{ data.item.subjectId }}
          </div>
        </template>
        <template v-slot:cell(exportedOn)="data">
          <date-cell :value="data.value" />
        </template>
      </skills-b-table>
    </b-card>

    <removal-validation v-if="removalValidation.show" v-model="removalValidation.show" @do-remove="doRemoveExportedSkill">
      <exported-skill-deletion-warning :skill-id="removalValidation.skillToRemove.skillId" />
    </removal-validation>
  </div>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import SkillsService from '@/components/skills/SkillsService';
  import DateCell from '@/components/utils/table/DateCell';
  import RemovalValidation from '@/components/utils/modal/RemovalValidation';
  import ExportedSkillDeletionWarning
    from '@/components/skills/catalog/ExportedSkillDeletionWarning';
  import CatalogService from '@/components/skills/catalog/CatalogService';

  export default {
    name: 'ExportedSkills',
    components: {
      ExportedSkillDeletionWarning,
      RemovalValidation,
      SkillsBTable,
      DateCell,
    },
    data() {
      return {
        projectId: this.$route.params.projectId,
        exportedSkills: [],
        removalValidation: {
          show: false,
          skillToRemove: {},
        },
        table: {
          options: {
            sortBy: 'exportedOn',
            sortDesc: true,
            busy: true,
            stacked: 'md',
            pagination: {
              server: true,
              currentPage: 1,
              totalRows: 1,
              pageSize: 5,
              possiblePageSizes: [5, 10, 25],
            },
            fields: [
              {
                key: 'skillName',
                label: 'Skill',
                sortable: true,
                sortKey: 'skillName',
              }, {
                key: 'subjectName',
                label: 'Subject',
                sortable: true,
                sortKey: 'subjectName',
              }, {
                key: 'exportedOn',
                label: 'Exported On',
                sortable: true,
                sortKey: 'exportedOn',
              },
            ],
          },
        },
      };
    },
    mounted() {
      this.loadExported();
    },
    watch: {
      '$route.params.projectId': function watcher() {
        this.projectId = this.$route.params.projectId;
      },
    },
    methods: {
      pageChanged(pageNum) {
        this.table.options.pagination.currentPage = pageNum;
        this.loadExported();
      },
      pageSizeChanged(newSize) {
        this.table.options.pagination.pageSize = newSize;
        this.loadExported();
      },
      sortTable(sortContext) {
        this.table.options.sortBy = sortContext.sortBy;
        this.table.options.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.table.options.pagination.currentPage = 1;
        this.loadExported();
      },
      loadExported() {
        this.table.options.busy = true;
        const pageParams = {
          limit: this.table.options.pagination.pageSize,
          ascending: !this.table.options.sortDesc,
          page: this.table.options.pagination.currentPage,
          orderBy: this.table.options.sortBy,
        };
        SkillsService.getSkillsExportedToCatalog(this.projectId, pageParams).then((res) => {
          if (res.data) {
            this.exportedSkills = res.data.map((skill) => ({ projectId: this.$route.params.projectId, ...skill }));
            this.table.options.pagination.totalRows = res.totalCount;
          } else {
            this.table.options.pagination.totalRows = 0;
          }
        }).finally(() => {
          this.table.options.busy = false;
        });
      },
      removeExported(skill) {
        this.removalValidation.skillToRemove = skill;
        this.removalValidation.show = true;
      },
      doRemoveExportedSkill() {
        this.table.options.busy = true;
        CatalogService.removeExportedSkill(this.removalValidation.skillToRemove.projectId, this.removalValidation.skillToRemove.skillId)
          .then(() => {
            this.loadExported();
          });
      },
    },
  };
</script>

<style scoped>

</style>
