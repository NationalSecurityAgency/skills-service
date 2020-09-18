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
  <div class="card mb-2">
    <div class="card-header">
      Achievements
    </div>
    <div class="card-body p-0">
      <div class="row p-3">
        <div class="col border-right">
          <b-form-group label="User Name Filter:" label-for="input-1" label-class="text-muted">
            <b-form-input id="user-name-filter" v-model="usernameFilter" v-on:keydown.enter="reloadTable"/>
          </b-form-group>
        </div>
        <div class="col border-right">
          <b-form-group label="From Date:" label-for="input-1" label-class="text-muted">
            <b-form-datepicker id="from-date-filter" v-model="fromDateFilter" class="mb-2"></b-form-datepicker>
          </b-form-group>
        </div>
        <div class="col border-right">
          <b-form-group label="To Date:" label-for="input-1" label-class="text-muted">
            <b-form-datepicker id="to-date-filter" v-model="toDateFilter" class="mb-2"></b-form-datepicker>
          </b-form-group>
        </div>
      </div>
      <div class="row px-3">
        <div class="col border-right">
          <b-form-group label="Types:" label-class="text-muted" >
            <b-form-checkbox-group
              id="checkbox-group-1"
              v-model="achievementTypes.selected"
              :options="achievementTypes.available"
              name="flavour-1"
            >
            </b-form-checkbox-group>
          </b-form-group>
        </div>
        <div class="col border-right">
          <b-form-group id="input-group-3" label="Minimum Level (Subject & Skill Only):" label-for="input-3" label-class="text-muted">
            <b-form-select id="input-3" v-model="levels.selected" :options="levels.available" required/>
          </b-form-group>
        </div>
        <div class="col">
          <b-form-group label="Name (Subject, Skill and Badge Only):" label-for="input-3" label-class="text-muted">
            <b-form-input id="name-filter" v-model="nameFilter" v-on:keydown.enter="reloadTable" />
          </b-form-group>
        </div>
      </div>
      <div class="row pl-3 mb-2">
        <div class="col">
          <b-button variant="outline-info" @click="reloadTable"><i class="fa fa-filter"/> Filter</b-button>
          <b-button variant="outline-info" @click="reset" class="ml-1"><i class="fa fa-times"/> Reset</b-button>
        </div>
      </div>

      <skills-b-table class="mb-5" :items="items" :options="tableOptions">
        <template v-slot:cell(username)="data">
          <span class="ml-2">{{ data.value }}</span>
          <b-button-group class="float-right">
            <b-button :to="{ name: 'ClientDisplayPreview', params: { projectId: projectId, userId: data.value } }"
                      variant="outline-info" size="sm" class="text-secondary"
                      v-b-tooltip.hover title="View User's Client Display"><i class="fa fa-eye"/></b-button>
            <b-button variant="outline-info" size="sm" class="text-secondary"
                      v-b-tooltip.hover title="View User's Metrics"><i class="fa fa-chart-bar"/></b-button>
          </b-button-group>
        </template>
        <template v-slot:cell(achievement)="data">
          <an-achievement :achievement="data.value" />
        </template>
        <template v-slot:cell(timestamp)="data">
          <span class="">{{ data.value | date }}</span>
          <b-badge v-if="isToday(data.value)" variant="info" class="ml-2">Today</b-badge>
          <div class="small text-muted">
            {{ relativeTime(data.value) }}
          </div>
        </template>
      </skills-b-table>
    </div>
  </div>
</template>

<script>
  import moment from 'moment';
  import SkillsBTable from '../../utils/table/SkillsBTable';
  import MetricsService from '../MetricsService';
  import AnAchievement from './AnAchievement';

  export default {
    name: 'AchievementsNavigator',
    components: { AnAchievement, SkillsBTable },
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
          sortBy: 'timestamp',
          sortDesc: true,
          bordered: true,
          outlined: true,
          rowDetailsControls: false,
          fields: [
            {
              key: 'userName',
              sortable: true,
              label: 'Username',
            },
            {
              key: 'achievement',
              sortable: false,
            },
            {
              key: 'timestamp',
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
