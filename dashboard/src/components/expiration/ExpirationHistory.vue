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
    <sub-page-header title="Skill Expiration History"/>
    <b-card body-class="p-0">
      <div class="row px-3 pt-3">
        <div class="col-md-6">
          <b-form-group label="Skill Name Filter" label-class="text-muted">
            <b-input v-model="filters.skillName" v-on:keydown.enter="applyFilters" data-cy="skillNameFilter" aria-label="skill name filter"/>
          </b-form-group>
        </div>
        <div class="col-md-6">
          <b-form-group label="User Filter" label-class="text-muted">
            <b-input v-model="filters.userIdForDisplay" v-on:keydown.enter="applyFilters" data-cy="userIdFilter" aria-label="user id filter"/>
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
          <template #head(userIdForDisplay)="data">
            <span class="text-primary">
              <i class="fas fa-user-cog skills-color-skills" aria-hidden="true"/> {{ data.label }}
            </span>
          </template>
          <template #head(skillName)="data">
            <span class="text-primary"><i class="fas fa-graduation-cap skills-color-skills" aria-hidden="true"></i> {{ data.label }}</span>
          </template>
          <template #head(expiredOn)="data">
            <span class="text-primary"><i class="fas fa-clock text-warning" aria-hidden="true"></i> {{ data.label }}</span>
          </template>
          <template v-slot:cell(userIdForDisplay)="data">
            {{ getUserDisplay(data.item, true) }}

            <b-button-group class="float-right">
              <b-button :to="calculateClientDisplayRoute(data)"
                        variant="outline-info" size="sm" class="text-secondary"
                        v-b-tooltip.hover="'View User Details'"
                        :aria-label="`View details for user ${data.label}`"
                        data-cy="usersTable_viewDetailsBtn"><i class="fa fa-user-alt" aria-hidden="true"/><span class="sr-only">view user details</span>
              </b-button>
            </b-button-group>
          </template>
          <template v-slot:cell(skillName)="data">
            <a :href="getUrl(data.item)">{{ data.value }}</a>
          </template>
          <template v-slot:cell(expiredOn)="data">
            <date-cell :value="data.value" />
          </template>
        </skills-b-table>
      </b-overlay>
    </b-card>
  </div>
</template>

<script>
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import ExpirationService from '@/components/expiration/ExpirationService';
  import DateCell from '../utils/table/DateCell';
  import UserIdForDisplayMixin from '../users/UserIdForDisplayMixin';

  export default {
    name: 'ExpirationHistory',
    mixins: [UserIdForDisplayMixin],
    components: {
      SubPageHeader,
      SkillsBTable,
      DateCell,
    },
    data() {
      return {
        projectId: this.$route.params.projectId,
        filters: {
          skillName: '',
          userIdForDisplay: '',
        },
        table: {
          options: {
            busy: false,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'userIdForDisplay',
            sortDesc: true,
            tableDescription: 'ExpirationHistory',
            fields: [
              {
                key: 'skillName',
                label: 'Skill Name',
                sortable: true,
              },
              {
                key: 'userIdForDisplay',
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
          skillName: this.filters.skillName,
          userIdForDisplay: this.filters.userIdForDisplay,
        };
        return ExpirationService.getExpiredSkills(this.$route.params.projectId, params).then((res) => {
          this.table.items = res.data;
          this.table.options.pagination.totalRows = res.totalCount;
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
          let filterMessage = 'Skill expiration history table has been filtered by';
          if (this.filters.skillName) {
            filterMessage += ` ${this.filters.skillName}`;
          }
          if (this.filters.userIdForDisplay) {
            filterMessage += ` ${this.filters.userIdForDisplay}`;
          }
          this.$nextTick(() => this.$announcer.polite(filterMessage));
        });
      },
      reset() {
        this.filters.userIdForDisplay = '';
        this.filters.skillName = '';
        this.loadData().then(() => {
          this.$nextTick(() => this.$announcer.polite('Skill expiration history table filters have been removed'));
        });
      },
      getUrl(item) {
        return `/administrator/projects/${encodeURIComponent(this.projectId)}/subjects/${encodeURIComponent(item.subjectId)}/skills/${encodeURIComponent(item.skillId)}/`;
      },
      calculateClientDisplayRoute(props) {
        const routeObj = {
          name: 'ClientDisplayPreview',
          params: {
            projectId: this.$route.params.projectId,
            userId: props.item.userId,
          },
        };

        return routeObj;
      },
    },
  };
</script>
