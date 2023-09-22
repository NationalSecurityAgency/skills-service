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
    <b-card body-class="p-0">
      <div class="row px-3 pt-3">
        <div class="col-md-6">
          <b-form-group label="Skill Id Filter" label-class="text-muted">
            <b-input v-model="filters.skillId" v-on:keydown.enter="applyFilters" data-cy="skillIdFilter" aria-label="skill id filter"/>
          </b-form-group>
        </div>
        <div class="col-md-6">
          <b-form-group label="User Id Filter" label-class="text-muted">
            <b-input v-model="filters.userId" v-on:keydown.enter="applyFilters" data-cy="userIdFilter" aria-label="user id filter"/>
          </b-form-group>
        </div>
      </div>
      <div class="row pl-3 mb-3">
        <div class="col">
          <b-button variant="outline-info" @click="applyFilters" data-cy="users-filterBtn"><i class="fa fa-filter" aria-hidden="true" /> Filter</b-button>
          <b-button variant="outline-info" @click="reset" class="ml-1" data-cy="users-resetBtn"><i class="fa fa-times" aria-hidden="true" /> Reset</b-button>
        </div>
      </div>
      <b-overlay :show="table.options.busy">
        <skills-b-table :options="table.options" :items="table.items"
                        @page-size-changed="pageSizeChanged"
                        @page-changed="pageChanged"
                        @sort-changed="sortTable"
                        tableStoredStateId="expirationHistoryTable"
                        data-cy="expirationHistoryTable">

        </skills-b-table>
      </b-overlay>
    </b-card>
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
        filters: {
          skillId: '',
          userId: '',
        },
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
          skillId: this.filters.skillId,
          userId: this.filters.userId,
        };
        ExpirationService.getExpiredSkills(this.$route.params.projectId, params).then((res) => {
          this.table.items = res;
        });
      },
      sortTable(sortContext) {
        this.table.options.sortBy = sortContext.sortBy;
        this.table.options.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.table.options.pagination.currentPage = 1;
        this.loadData();
      },
      pageChanged(pageNum) {
        this.table.options.pagination.currentPage = pageNum;
        this.loadData();
      },
      pageSizeChanged(newSize) {
        this.table.options.pagination.pageSize = newSize;
        this.loadData();
      },
      applyFilters() {
        this.table.options.pagination.currentPage = 1;
        this.loadData().then(() => {
          let filterMessage = 'Expiration history table has been filtered by';
          if (this.filters.skillId) {
            filterMessage += ` ${this.filters.skillId}`;
          }
          if (this.filters.userId) {
            filterMessage += ` ${this.filters.userId}`;
          }
          this.$nextTick(() => this.$announcer.polite(filterMessage));
        });
      },
      reset() {
        this.filters.userId = '';
        this.filters.skillId = '';
        this.loadData().then(() => {
          this.$nextTick(() => this.$announcer.polite('Expiration history table filters have been removed'));
        });
      },
    },
  };
</script>
