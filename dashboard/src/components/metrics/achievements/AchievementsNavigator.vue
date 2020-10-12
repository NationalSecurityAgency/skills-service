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
  <metrics-card title="Achievements" :no-padding="true" data-cy="achievementsNavigator">
      <div class="row p-3">
        <div class="col-md border-right">
          <b-form-group label="User Name Filter:" label-for="input-1" label-class="text-muted">
            <b-form-input id="user-name-filter" v-model="usernameFilter" v-on:keydown.enter="reloadTable" data-cy="achievementsNavigator-usernameInput"/>
          </b-form-group>
        </div>
        <div class="col-6 col-md border-right">
          <b-form-group label="From Date:" label-for="input-1" label-class="text-muted">
            <b-form-datepicker id="from-date-filter" v-model="fromDateFilter" class="mb-2" data-cy="achievementsNavigator-fromDateInput"></b-form-datepicker>
          </b-form-group>
        </div>
        <div class="col-6 col-md border-right">
          <b-form-group label="To Date:" label-for="input-1" label-class="text-muted">
            <b-form-datepicker id="to-date-filter" v-model="toDateFilter" class="mb-2" data-cy="achievementsNavigator-toDateInput"></b-form-datepicker>
          </b-form-group>
        </div>
      </div>
      <div class="row px-3">
        <div class="col-xl border-right">
          <b-form-group label="Types:" label-class="text-muted" data-cy="achievementsNavigator-typeInput">
            <b-form-checkbox-group
              id="checkbox-group-1"
              v-model="achievementTypes.selected"
              :options="achievementTypes.available"
              name="flavour-1"
            >
            </b-form-checkbox-group>
          </b-form-group>
        </div>
        <div class="col-12 col-md-6 col-xl border-right">
          <b-form-group id="levels-input-group" label="Minimum Level (Subject & Skill Only):" label-for="input-3" label-class="text-muted">
            <b-form-select id="input-3" v-model="levels.selected" :options="levels.available" required data-cy="achievementsNavigator-levelsInput"/>
          </b-form-group>
        </div>
        <div class="col-12 col-md-6 col-xl">
          <b-form-group label="Name (Subject, Skill and Badge Only):" label-for="input-3" label-class="text-muted">
            <b-form-input id="name-filter" v-model="nameFilter" v-on:keydown.enter="reloadTable" data-cy="achievementsNavigator-nameInput"/>
          </b-form-group>
        </div>
      </div>
      <div class="row pl-3 mb-2">
        <div class="col">
          <b-button variant="outline-info" @click="reloadTable" data-cy="achievementsNavigator-filterBtn"><i class="fa fa-filter"/> Filter</b-button>
          <b-button variant="outline-info" @click="reset" class="ml-1" data-cy="achievementsNavigator-resetBtn"><i class="fa fa-times"/> Reset</b-button>
        </div>
      </div>

      <skills-b-table class="mb-5" data-cy="achievementsNavigator-table"
                      :items="items" :options="tableOptions"  @sort-changed="sortTable">
        <template v-slot:cell(username)="data">
          <div class="row">
            <div class="col-12 col-md-8">
              <span>{{ data.value }}</span>
            </div>
            <div class="col-12 col-md-4 text-md-right">
              <b-button-group>
                <b-button :to="{ name: 'ClientDisplayPreview', params: { projectId: projectId, userId: data.value } }"
                          variant="outline-info" size="sm" class="text-secondary"
                          v-b-tooltip.hover title="View User's Client Display"><i class="fa fa-eye"/></b-button>
                <b-button variant="outline-info" size="sm" class="text-secondary"
                          v-b-tooltip.hover title="View User's Metrics"><i class="fa fa-chart-bar"/></b-button>
              </b-button-group>
            </div>
          </div>
        </template>
        <template v-slot:cell(type)="data">
          <achievement-type :type="data.value" />
        </template>
        <template v-slot:cell(name)="data" data-cy="achievementsNavigator-table-skillName">
          <span v-if="data.value == 'Overall'" class="small text-muted">
            N/A
          </span>
          <span v-else>{{ data.value }}</span>
        </template>
        <template v-slot:cell(level)="data">
          <span v-if="!data.value" class="small text-muted">
            N/A
          </span>
          <span v-else>{{ data.value }}</span>
        </template>
        <template v-slot:cell(achievedOn)="data">
          <span class="">{{ data.value | date }}</span>
          <b-badge v-if="isToday(data.value)" variant="info" class="ml-2">Today</b-badge>
          <div class="small text-muted">
            {{ relativeTime(data.value) }}
          </div>
        </template>
      </skills-b-table>
  </metrics-card>
</template>

<script>
  import moment from 'moment';
  import SkillsBTable from '../../utils/table/SkillsBTable';
  import MetricsService from '../MetricsService';
  import AchievementType from './AchievementType';
  import MetricsCard from '../utils/MetricsCard';

  export default {
    name: 'AchievementsNavigator',
    components: { MetricsCard, AchievementType, SkillsBTable },
    mounted() {
      this.reloadTable();
    },
    data() {
      return {
        isLoading: true,
        projectId: this.$route.params.projectId,
        usernameFilter: '',
        fromDateFilter: '',
        toDateFilter: '',
        nameFilter: '',
        levels: {
          selected: '',
          available: [
            { value: '', text: 'Optionally select level' },
            { value: 1, text: 'Level 1' },
            { value: 2, text: 'Level 2' },
            { value: 3, text: 'Level 3' },
            { value: 4, text: 'Level 4' },
            { value: 5, text: 'Level 5' },
          ],
        },
        achievementTypes: {
          selected: ['Overall', 'Subject', 'Skill', 'Badge'],
          available: ['Overall', 'Subject', 'Skill', 'Badge'],
        },
        tableOptions: {
          busy: true,
          sortBy: 'achievedOn',
          sortDesc: true,
          bordered: true,
          outlined: true,
          rowDetailsControls: false,
          stacked: 'md',
          fields: [
            {
              key: 'userName',
              sortable: true,
              label: 'Username',
            },
            {
              key: 'type',
              sortable: false,
            },
            {
              key: 'name',
              sortable: false,
            },
            {
              key: 'level',
              sortable: false,
            },
            {
              key: 'achievedOn',
              label: 'Date',
              sortable: true,
            },
          ],
          pagination: {
            currentPage: 1,
            totalRows: 0,
            pageSize: 5,
            possiblePageSizes: [5, 10, 15, 20, 50],
          },
        },
        items: [],
      };
    },
    methods: {
      sortTable(sortContext) {
        this.tableOptions.sortBy = sortContext.sortBy;
        this.tableOptions.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.tableOptions.pagination.currentPage = 1;
        this.reloadTable();
      },
      reset() {
        this.usernameFilter = '';
        this.tableOptions.pagination.currentPage = 1;
        this.fromDateFilter = '';
        this.toDateFilter = '';
        this.nameFilter = '';
        this.levels.selected = '';
        this.achievementTypes.selected = this.achievementTypes.available;
        this.reloadTable();
      },
      reloadTable() {
        this.tableOptions.busy = true;
        const params = {
          pageSize: this.tableOptions.pagination.pageSize,
          currentPage: this.tableOptions.pagination.currentPage,
          usernameFilter: this.usernameFilter,
          fromDateFilter: this.fromDateFilter,
          toDateFilter: this.toDateFilter,
          nameFilter: this.nameFilter,
          minLevel: this.levels.selected,
          achievementTypes: this.achievementTypes.selected,
          sortBy: this.tableOptions.sortBy,
          sortDesc: this.tableOptions.sortDesc,
        };

        MetricsService.loadChart(this.$route.params.projectId, 'userAchievementsChartBuilder', params)
          .then((dataFromServer) => {
            this.isLoading = false;
            this.items = dataFromServer.items;
            this.tableOptions.pagination.totalRows = dataFromServer.totalNumItems;
            this.tableOptions.busy = false;
          });
      },
      isToday(timestamp) {
        return moment(timestamp)
          .isSame(new Date(), 'day');
      },
      relativeTime(timestamp) {
        return moment(timestamp)
          .startOf('hour')
          .fromNow();
      },
    },
    watch: {
      'tableOptions.pagination.pageSize': function pageSizeUpdate() {
        if (this.tableOptions.pagination.currentPage > 1) {
          // will reload the table in currentPage watch
          this.tableOptions.pagination.currentPage = 1;
        } else {
          this.reloadTable();
        }
      },
      'tableOptions.pagination.currentPage': function currentPageUpdate() {
        this.reloadTable();
      },
    },
  };
</script>

<style lang="scss" scoped>
@import "node_modules/bootstrap/scss/bootstrap";

.customPagination /deep/ button {
  color: $info !important;
  border-color: $secondary !important;
}

.customPagination /deep/ .disabled > .page-link {
  border-color: $secondary !important;
}

.customPagination /deep/ .active > button {
  background-color: $info !important;
  color: $white !important;
}

</style>
