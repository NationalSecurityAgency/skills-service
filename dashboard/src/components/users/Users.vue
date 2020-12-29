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
  <div class="usersTable">
    <sub-page-header title="Users"/>

    <b-card body-class="p-0">
      <div class="row px-3 pt-3">
        <div class="col-12">
          <b-form-group label="User Id Filter" label-class="text-muted">
            <b-input v-model="filters.userId" data-cy="users-skillIdFilter" aria-label="skill id filter"/>
          </b-form-group>
        </div>
        <div class="col-md">
        </div>
      </div>

      <div class="row pl-3 mb-3">
        <div class="col">
          <b-button variant="outline-info" @click="applyFilters" data-cy="users-filterBtn"><i class="fa fa-filter"/> Filter</b-button>
          <b-button variant="outline-info" @click="reset" class="ml-1" data-cy="users-resetBtn"><i class="fa fa-times"/> Reset</b-button>
        </div>
      </div>

      <skills-b-table :options="table.options" :items="table.items"
                      @page-changed="pageChanged"
                      @page-size-changed="pageSizeChanged"
                      @sort-changed="sortTable"
                      data-cy="usersTable">
        <template v-slot:cell(userId)="data">
          {{ data.value }}

          <b-button-group class="float-right">
            <b-button :to="calculateClientDisplayRoute(data.item)"
                      variant="outline-info" size="sm" class="text-secondary"
                      v-b-tooltip.hover="'View User Details'"
                      data-cy="usersTable_viewDetailsBtn"><i class="fa fa-user-alt"/><span class="sr-only">view user details</span>
            </b-button>
          </b-button-group>
        </template>
        <template v-slot:cell(totalPoints)="data">
          {{ data.value | number }}
        </template>
        <template v-slot:cell(lastUpdated)="data">
          <div>
            <span>{{ data.value | date }}</span>
            <b-badge v-if="isToday(data.value)" variant="info" class="ml-2">Today</b-badge>
          </div>
          <div class="text-muted small">
            {{ data.value | timeFromNow }}
          </div>
        </template>
      </skills-b-table>
    </b-card>
  </div>
</template>

<script>
  import axios from 'axios';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import dayjs from '../../DayJsCustomizer';
  import UsersService from './UsersService';

  export default {
    name: 'Users',
    components: {
      SkillsBTable,
      SubPageHeader,
    },
    data() {
      const self = this;
      return {
        loading: true,
        initialLoad: true,
        data: [],
        totalNumUsers: '...',
        filters: {
          userId: '',
        },
        table: {
          items: [],
          options: {
            busy: true,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'userId',
            sortDesc: false,
            fields: [
              {
                key: 'userId',
                label: 'User Id',
                sortable: true,
              },
              {
                key: 'totalPoints',
                label: 'Total Points',
                sortable: true,
              },
              {
                key: 'lastUpdated',
                label: 'Last Reported Skill',
                sortable: true,
              },
            ],
            pagination: {
              server: true,
              currentPage: 1,
              totalRows: 1,
              pageSize: 5,
              possiblePageSizes: [5, 10, 15, 20],
            },
          },
        },
        columns: ['userId', 'totalPoints', 'lastUpdated', 'viewDetail'],
        options: {
          headings: {
            userId: 'User',
            totalPoints: 'Total Points',
            lastUpdated: 'Last Reported Skill',
            viewDetail: '',
          },
          columnsClasses: {
            viewDetail: 'control-column',
          },
          sortable: ['userId', 'totalPoints', 'lastUpdated'],
          orderBy: {
            column: 'userId',
            ascending: true,
          },
          dateColumns: ['lastUpdated'],
          dateFormat: 'YYYY-MM-DD HH:mm',
          sortIcon: {
            base: 'fa fa-sort', up: 'fa fa-sort-up', down: 'fa fa-sort-down', is: 'fa fa-sort',
          },
          filterable: true,
          highlightMatches: true,
          skin: 'table is-striped is-fullwidth',
          /* eslint-disable */
          requestFunction: function (data) {
            return axios.get(this.url, {
              params: data
            }).then((res) => {
              self.loading = false;
              self.totalNumUsers = res.data.totalCount;
              return res;
            }).catch(function (e) {
              self.loading = false;
              self.totalNumUsers = 0;
              this.dispatch('error', e);
            }.bind(this));
          },
          /* eslint-enable */
        },
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      isToday(timestamp) {
        return dayjs(timestamp)
          .isSame(new Date(), 'day');
      },
      pageChanged(pageNum) {
        this.table.options.pagination.currentPage = pageNum;
        this.loadData();
      },
      pageSizeChanged(newSize) {
        this.table.options.pagination.pageSize = newSize;
        this.loadData();
      },
      sortTable(sortContext) {
        this.table.options.sortBy = sortContext.sortBy;
        this.table.options.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.table.options.pagination.currentPage = 1;
        this.loadData();
      },
      applyFilters() {
        this.table.options.pagination.currentPage = 1;
        this.loadData();
      },
      reset() {
        this.filters.userId = '';
        this.table.options.pagination.currentPage = 1;
        this.loadData();
      },
      loadData() {
        this.table.options.busy = true;
        const url = this.getUrl();
        UsersService.ajaxCall(url, {
          query: this.filters.userId,
          limit: this.table.options.pagination.pageSize,
          ascending: !this.table.options.sortDesc,
          page: this.table.options.pagination.currentPage,
          byColumn: 0,
          orderBy: this.table.options.sortBy,
        }).then((res) => {
          this.table.items = res.data;
          this.table.options.pagination.totalRows = res.count;
          this.table.options.busy = false;
        });
      },
      calculateClientDisplayRoute(props) {
        const hasSubject = this.$route.params.subjectId || false;
        const hasSkill = this.$route.params.skillId || false;
        const hasBadge = this.$route.params.badgeId || false;

        let routeObj = {
          name: 'ClientDisplayPreview',
          params: {
            projectId: this.$route.params.projectId,
            userId: props.userId,
            dn: props.dn,
          },
        };

        if (hasSkill) {
          routeObj = {
            name: 'ClientDisplayPreviewSkill',
            params: {
              projectId: this.$route.params.projectId,
              subjectId: this.$route.params.subjectId,
              skillId: this.$route.params.skillId,
              userId: props.userId,
              dn: props.dn,
            },
          };
        } else if (hasSubject) {
          routeObj = {
            name: 'ClientDisplayPreviewSubject',
            params: {
              projectId: this.$route.params.projectId,
              subjectId: this.$route.params.subjectId,
              userId: props.userId,
              dn: props.dn,
            },
          };
        } else if (hasBadge) {
          routeObj = {
            name: 'ClientDisplayPreviewBadge',
            params: {
              projectId: this.$route.params.projectId,
              badgeId: this.$route.params.badgeId,
              userId: props.userId,
              dn: props.dn,
            },
          };
        }

        return routeObj;
      },
      onLoading() {
        this.loading = true;
      },
      onLoaded(event) {
        this.loading = false;
        this.initialLoad = false;
        this.$emit('loaded', event);
      },
      getUrl() {
        let url = `/admin/projects/${this.$route.params.projectId}`;
        if (this.$route.params.skillId) {
          url += `/skills/${this.$route.params.skillId}`;
        } else if (this.$route.params.badgeId) {
          url += `/badges/${this.$route.params.badgeId}`;
        } else if (this.$route.params.subjectId) {
          url += `/subjects/${this.$route.params.subjectId}`;
        }
        url += '/users';
        return url;
      },
      emit(name, event) {
        this.$emit(name, event, this);
      },
      clear() {
        this.$refs.table.data = [];
        this.$refs.table.count = 0;
      },
      getDate(props) {
        return dayjs(props.row.lastUpdated).format('LLL');
      },
      getUserDisplay(props) {
        return props.row.lastName && props.row.firstName ? `${props.row.firstName} ${props.row.lastName} (${props.row.userIdForDisplay})` : props.row.userIdForDisplay;
      },
    },
  };
</script>

<style>
  .usersTable .control-column{
    width: 8rem;
  }
  /* on the mobile platform some of the columns will be removed
   so let's allow the table to size on its own*/
  @media (max-width: 576px) {
    .usersTable .control-column {
      width: unset;
    }
  }

  .usersTable table {
    width: 100%;
    position: relative;
  }
</style>
