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
      <h5>Achievements Navigator</h5>
    </div>
    <div class="card-body p-0">
      <div class="row p-3">
        <div class="col border-right">
          <b-form-group id="input-group-1" label="User Name Filter:" label-for="input-1" label-class="text-muted">
            <b-form-input id="input-1" v-model="usernameFilter" type="email"/>
          </b-form-group>
        </div>
        <div class="col border-right">
          <b-form-group id="input-group-3" label="Level:" label-for="input-3" label-class="text-muted">
            <b-form-select id="input-3" v-model="levels.selected" :options="levels.available" required/>
          </b-form-group>
        </div>
        <div class="col">
          <b-form-group id="badgesGroup" label="Badges:" label-for="input-3" label-class="text-muted">
            <b-form-select id="input-3" v-model="badges.selected" :options="badges.available" required/>
          </b-form-group>
        </div>
      </div>

      <skills-b-table class="mb-5" :items="items" :options="tableOptions">
        <template v-slot:cell(user_name)="data">
          <b-button-group>
            <b-button :to="{ name: 'ClientDisplayPreview', params: { projectId: projectId, userId: data.value } }"
                      variant="outline-info" size="sm" class="text-secondary"
                      v-b-tooltip.hover title="View User's Client Display"><i class="fa fa-eye"/></b-button>
            <b-button variant="outline-info" size="sm" class="text-secondary"
                      v-b-tooltip.hover title="View User's Metrics"><i class="fa fa-chart-bar"/></b-button>
          </b-button-group>
          <span class="ml-2">{{ data.value }}</span>
        </template>
        <template v-slot:cell(achievement)="data">
          <span class="border border-info rounded d-inline-block bg-white" style="width: 2rem; text-align: center">
            <i class="fa fa-trophy text-muted" v-if="data.value.startsWith('Level')"/>
            <i class="fa fa-award text-muted" v-else/>
          </span>
          <span class="ml-2">{{ data.value }}</span>
        </template>
        <template v-slot:cell(timestamp)="data">
          <span class="">{{ data.value | date }}</span>
          <b-badge v-if="isToday(data.value)" variant="info" class="ml-2">Today</b-badge>
        </template>
      </skills-b-table>
    </div>
  </div>
</template>

<script>
  import moment from 'moment';
  import SkillsBTable from '../../utils/table/SkillsBTable';

  export default {
    name: 'MetricsTable',
    components: { SkillsBTable },
    data() {
      return {
        projectId: this.$route.params.projectId,
        usernameFilter: '',
        badges: {
          selected: 'All Badges',
          available: ['All Badges', 'Remove Badges', 'Badge 1', 'Badge 2'],
        },
        levels: {
          selected: 'All Levels',
          available: ['All Levels', 'Level 1', 'Level 2', 'Level 3', 'Level 4', 'Level 5'],
        },
        tableOptions: {
          busy: false,
          sortBy: 'timestamp',
          sortDesc: true,
          fields: [
            {
              key: 'user_name',
              sortable: true,
              label: 'Username',
            },
            {
              key: 'achievement',
              sortable: true,
            },
            {
              key: 'timestamp',
              label: 'Date',
              sortable: true,
            },
          ],
          pagination: {
            currentPage: 1,
            totalRows: 76,
            perPage: 5,
            possiblePageSizes: [5, 10, 15, 20, 50],
          },
        },
        items: [
          {
            timestamp: 1599824550435,
            user_name: 'DickersonMacdonald@gmail.com',
            achievement: 'Level 1',
          },
          {
            timestamp: 1599824550435,
            user_name: 'dafeafeafe@gmail.com',
            achievement: 'Level 2',
          },
          {
            timestamp: 313442323,
            user_name: 'aefaefa@gmail.com',
            achievement: 'Level 1',
          },
          {
            timestamp: 1313131313,
            user_name: 'eafeafea@gmail.com',
            achievement: 'Some Badge',
          },
          {
            timestamp: 4422442242,
            user_name: 'eafaefafe@gmail.com',
            achievement: 'Level 1',
          },
        ],
      };
    },
    methods: {
      isToday(timestamp) {
        return moment(timestamp)
          .isSame(new Date(), 'day');
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
