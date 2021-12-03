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

      <skills-spinner :is-loading="loading" />

      <skills-b-table v-if="!loading"
                      :options="table.options"
                      :items="exportedSkills"
                      data-cy="exportedSkills"
                      @page-changed="pageChanged"
                      @page-size-changed="pageSizeChanged"
                      @sort-changed="sortTable">
        <template v-slot:cell(exportedOn)="data">
          <date-cell :value="data.value" />
        </template>
      </skills-b-table>
    </b-card>
  </div>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import SkillsService from '@/components/skills/SkillsService';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import DateCell from '@/components/utils/table/DateCell';

  export default {
    name: 'ExportedSkills',
    components: {
      SkillsBTable,
      SkillsSpinner,
      DateCell,
    },
    data() {
      return {
        projectId: this.$route.params.projectId,
        loading: true,
        exportedSkills: [],
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
                label: 'Name',
                sortable: true,
                sortKey: 'skillName',
              }, {
                key: 'exportedOn',
                label: 'Exported On',
                sortable: true,
                sortKey: 'exportedOn',
              }, {
                key: 'subjectName',
                label: 'Subject Name',
                sortable: true,
                sortKey: 'subjectName',
              },
            ],
          },
        },
      };
    },
    mounted() {
      console.log(`this.$route.params.projectId: ${this.$route.params.projectId}`);
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
        const pageParams = {
          limit: this.table.options.pagination.pageSize,
          ascending: !this.table.options.sortDesc,
          page: this.table.options.pagination.currentPage,
          orderBy: this.table.options.sortBy,
        };
        this.loading = true;
        SkillsService.getSkillsExportedToCatalog(this.projectId, pageParams).then((data) => {
          this.exportedSkills = data;
        }).finally(() => {
          this.loading = false;
          this.table.options.busy = false;
        });
      },
    },
  };
</script>

<style scoped>

</style>
