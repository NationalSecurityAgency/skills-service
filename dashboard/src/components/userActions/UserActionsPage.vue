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
    <sub-page-header title="Dashboard Activity History" />
    <b-card body-class="p-0">
      <b-overlay :show="table.options.busy || this.options.loading">
        <div class="p-2 py-3">
        <div class="row px-3 pt-1">
          <div class="col-md border-right">
            <b-form-group label="User:" label-for="user-filter" label-class="text-muted">
              <b-form-input id="user-filter" v-model="filters.user"
                            v-on:keydown.enter="loadData"
                            maxlength="50"
                            data-cy="userFilter"/>
            </b-form-group>
          </div>
          <div class="col-md border-right">
            <b-form-group label="Action:" label-for="action-filter" label-class="text-muted">
              <b-form-select id="action-filter"
                             v-model="filters.action"
                             @input="loadData"
                             :options="options.actions"
                             data-cy="actionFilter"/>
            </b-form-group>
          </div>
          <div class="col-md">
            <b-form-group label="Item:" label-for="item-filter" label-class="text-muted">
              <b-form-select id="item-filter"
                             v-model="filters.item"
                             @input="loadData"
                             :options="options.items"
                             data-cy="itemFilter"/>
            </b-form-group>
          </div>
        </div>
        <div class="row px-3 pt-1">
          <div class="col-md border-right">
            <b-form-group label="Item ID:" label-for="item-id-filter" label-class="text-muted">
              <b-form-input id="item-id-filter" v-model="filters.itemId"
                            v-on:keydown.enter="loadData"
                            maxlength="50"
                            data-cy="itemIdFilter"/>
            </b-form-group>
          </div>
          <div class="col-md border-right">
            <b-form-group label="Project ID:" label-for="project-id-filter" label-class="text-muted">
              <b-form-input id="project-id-filter" v-model="filters.projectId"
                            v-on:keydown.enter="loadData"
                            maxlength="50"
                            data-cy="projectIdFilter"/>
            </b-form-group>
          </div>
          <div class="col-md">
            <b-form-group label="Quiz ID:" label-for="quiz-id-filter" label-class="text-muted">
              <b-form-input id="quiz-id-filter" v-model="filters.quizId"
                            v-on:keydown.enter="loadData"
                            maxlength="50"
                            data-cy="quizIdFilter"/>
            </b-form-group>
          </div>
        </div>

        <div class="row px-3 mb-3 mt-2">
          <div class="col">
            <div class="pr-2 d-inline-block">
              <b-button variant="outline-primary" @click="loadData"
                        class="mt-1" data-cy="filterBtn"><i
                class="fa fa-filter"/> Filter
              </b-button>
              <b-button variant="outline-primary" @click="reset" class="ml-1 mt-1" data-cy="filterResetBtn"><i class="fa fa-times"/> Reset</b-button>
            </div>
          </div>
        </div>
      </div>
      </b-overlay>
      <skills-b-table :options="table.options" :items="table.items"
                      @page-size-changed="pageSizeChanged"
                      @page-changed="pageChanged"
                      @sort-changed="sortTable"
                      tableStoredStateId="dashboardActionsForEverythingTable"
                      data-cy="dashboardActionsForEverything">
        <template #head(userIdForDisplay)="data">
          <span class="text-primary"><i
            class="fas fa-user-cog skills-color-skills" aria-hidden="true"/> {{ data.label }}</span>
        </template>
        <template #head(action)="data">
          <span class="text-primary"><i
            class="fas fa-stamp text-success" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(item)="data">
          <span class="text-primary"><i
            class="fas fa-clipboard-check skills-color-subjects" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(itemId)="data">
          <span class="text-primary"><i class="fas fa-fingerprint skills-color-points" aria-hidden="true"></i> {{
              data.label
            }}</span>
        </template>
        <template #head(projectId)="data">
          <span class="text-primary"><i class="fas fa-tasks skills-color-projects" aria-hidden="true"></i> {{
              data.label
            }}</span>
        </template>
        <template #head(quizId)="data">
          <span class="text-primary"><i class="fas fa-spell-check skills-color-subjects" aria-hidden="true"></i> {{
              data.label
            }}</span>
        </template>
        <template #head(created)="data">
          <span class="text-primary"><i class="fas fa-clock text-warning" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template v-slot:cell(controls)="data">
          <b-button variant="outline-primary"
                    size="sm"
                    @click="data.toggleDetails"
                    :data-cy="`row${data.index}-toggleDetails`"
                    :aria-label="`Expand details for row ${data.index}`"
                    class="mr-2 px-1 py-0">
            <i class="fas" :class="{'fa-plus': !data.detailsShowing, 'fa-minus' : data.detailsShowing}"></i>
          </b-button>
        </template>

        <template v-slot:cell(userIdForDisplay)="data">
          <span :data-cy="`row${data.index}-userId`">{{ data.value }}</span>
        </template>

        <template v-slot:cell(action)="data">
          <span :data-cy="`row${data.index}-action`">{{ formatLabel(data.value) }}</span>
        </template>

        <template v-slot:cell(item)="data">
          <span :data-cy="`row${data.index}-item`">{{ formatLabel(data.value) }}</span>
        </template>

        <template v-slot:cell(itemId)="data">
          <span :data-cy="`row${data.index}-itemId`">{{ data.value }}</span>
        </template>
        <template v-slot:cell(projectId)="data">
          <span :data-cy="`row${data.index}-projectId`">{{ data.value }}</span>
        </template>
        <template v-slot:cell(quizId)="data">
          <span :data-cy="`row${data.index}-quizId`">{{ data.value }}</span>
        </template>

        <template v-slot:cell(created)="data">
          <date-cell :value="data.value"/>
        </template>

        <template #row-details="row">
          <single-user-action :data-cy="`row${row.index}-expandedDetails`"
            :action-id="row.item.id" :item="row.item.item" :action="row.item.action"/>
        </template>
      </skills-b-table>
    </b-card>
  </div>
</template>

<script>
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import UserActionsService from '@/components/userActions/UserActionsService';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import DateCell from '@/components/utils/table/DateCell';
  import SingleUserAction from '@/components/userActions/SingleUserAction';

  export default {
    name: 'UserActionsPage',
    components: {
      SingleUserAction, DateCell, SkillsBTable, SubPageHeader,
    },
    data() {
      return {
        options: {
          loading: true,
          actions: [],
          items: [],
        },
        filters: {
          user: '',
          action: '',
          item: '',
          itemId: '',
          projectId: '',
          quizId: '',
        },
        table: {
          options: {
            busy: false,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'created',
            sortDesc: true,
            tableDescription: 'UserActions',
            fields: [
              {
                key: 'controls',
                label: '',
                sortable: false,
                thStyle: { width: '2rem' },
              },
              {
                key: 'userIdForDisplay',
                label: 'User',
                sortable: true,
              },
              {
                key: 'action',
                label: 'Action',
                sortable: true,
              },
              {
                key: 'item',
                label: 'Item',
                sortable: true,
              },
              {
                key: 'itemId',
                label: 'Item ID',
                sortable: true,
              },
              {
                key: 'projectId',
                label: 'Project ID',
                sortable: true,
              },
              {
                key: 'quizId',
                label: 'Quiz ID',
                sortable: true,
              },
              {
                key: 'created',
                label: 'Performed',
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
      this.loadFilterOptions();
    },
    methods: {
      reset() {
        this.filters.item = '';
        this.filters.user = '';
        this.filters.action = '';
        this.filters.itemId = '';
        this.filters.projectId = '';
        this.filters.quizId = '';
        this.loadData();
      },
      loadFilterOptions() {
        this.options.loading = true;
        UserActionsService.getDashboardActionsFilterOptions()
          .then((res) => {
            this.options.actions = res.actionFilterOptions.map((val) => ({ text: this.formatLabel(val), value: val }));
            this.options.items = res.itemFilterOptions.map((val) => ({ text: this.formatLabel(val), value: val }));
          }).finally(() => {
            this.options.loading = false;
          });
      },
      loadData() {
        this.table.options.busy = true;
        const params = {
          limit: this.table.options.pagination.pageSize,
          page: this.table.options.pagination.currentPage,
          orderBy: this.table.options.sortBy,
          ascending: !this.table.options.sortDesc,
          projectIdFilter: encodeURIComponent(this.filters.projectId.trim()),
          itemFilter: encodeURIComponent(this.filters.item.trim()),
          userFilter: encodeURIComponent(this.filters.user.trim()),
          quizFilter: encodeURIComponent(this.filters.quizId.trim()),
          itemIdFilter: encodeURIComponent(this.filters.itemId.trim()),
          actionFilter: encodeURIComponent(this.filters.action.trim()),
        };
        UserActionsService.getDashboardActionsForEverything(params)
          .then((res) => {
            this.table.options.pagination.totalRows = res.totalCount;
            this.table.items = res.data;
          }).finally(() => {
            this.table.options.busy = false;
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
      formatLabel(originalLabel) {
        return originalLabel
          .replace(/([A-Z])/g, (match) => ` ${match}`)
          .replace(/^./, (match) => match.toUpperCase());
      },
    },
  };
</script>

<style scoped>

</style>
