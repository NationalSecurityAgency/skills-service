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
    <sub-page-header title="Exported Skills">
      <!--div class="row">
        <div class="col">
          <b-tooltip target="remove-button" title="Remove all project errors." :disabled="errors.length < 1"></b-tooltip>
          <span id="remove-button" class="mr-2">
            <b-button variant="outline-primary" ref="removeAllErrors" @click="removeAllErrors" :disabled="errors.length < 1" size="sm"
                      data-cy="removeAllErrors">
              <span class="d-none d-sm-inline">Remove</span> All <i class="text-warning fas fa-trash-alt" aria-hidden="true"/>
            </b-button>
          </span>
        </div>
      </div-->
    </sub-page-header>

    <b-card body-class="p-0">
  <!-- Going to want stat cards that show how many skills are exported, maybe another that shows how many of those are in use by other projects
      maybe number of projects using exported skills as well
  -->
      <skills-spinner :is-loading="loading" />

      <skills-b-table v-if="!loading"
                      :options="table.options"
                      :items="exportedSkills"
                      data-cy="exportedSkills"
                      @page-changed="pageChanged"
                      @page-size-changed="pageSizeChanged"
                      @sort-changed="sortTable">

      </skills-b-table>
    </b-card>
  </div>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import SkillsService from '@/components/skills/SkillsService';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  /* import DateCell from '@/components/utils/table/DateCell'; */

  export default {
    name: 'ExportedSkills',
    components: {
      SkillsBTable,
      SkillsSpinner,
      SubPageHeader,
      /* DateCell, */
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
