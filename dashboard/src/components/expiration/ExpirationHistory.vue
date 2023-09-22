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
  <div>
    <sub-page-header title="Expiration History"/>

    <skills-b-table :options="table.options" :items="table.items"
                    tableStoredStateId="expirationHistoryTable"
                    data-cy="expirationHistoryTable">
    </skills-b-table>
  </div>
</template>

<script>
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import ExpirationService from '@/components/expiration/ExpirationService';

  export default {
    name: 'ExpirationHistory',
    components: {
      SubPageHeader,
      SkillsBTable,
    },
    data() {
      return {
        table: {
          options: {
            busy: false,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'userId',
            sortDesc: true,
            tableDescription: 'ExpirationHistory',
            fields: [
              {
                key: 'skillId',
                label: 'Skill',
                sortable: true,
              },
              {
                key: 'userId',
                label: 'User',
                sortable: true,
              },
              {
                key: 'expiredOn',
                label: 'Expired On',
                sortable: true,
              },
            ],
            pagination: {
              server: true,
              currentPage: 1,
              totalRows: 1,
              pageSize: 10,
              possiblePageSizes: [10, 25, 50],
            },
          },
          items: [],
        },
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      loadData() {
        const params = {
          limit: this.table.options.pagination.pageSize,
          page: this.table.options.pagination.currentPage,
          orderBy: this.table.options.sortBy,
          ascending: !this.table.options.sortDesc,
        };
        ExpirationService.getExpiredSkills(this.$route.params.projectId, params).then((res) => {
          this.table.items = res;
        });
      },
    },
  };
</script>
