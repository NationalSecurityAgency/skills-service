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
  <metrics-card title="Skills" :no-padding="true" data-cy="skillsNavigator">
    <div class="row px-3 pt-3">
      <div class="col-12 col-md border-right">
        <b-form-group label="Skill Name Filter" label-class="text-muted">
          <b-input v-model="filters.name" data-cy="skillsNavigator-skillNameFilter"/>
        </b-form-group>
      </div>
      <div class="col-md border-right" data-cy="skillsNavigator-filters">
        <b-form-group label="Tag Filters"  label-class="text-muted">
          <b-form-checkbox v-model="filters.overlookedTag" inline>
            <b-badge variant="danger" class="ml-2">Overlooked Skill</b-badge>
          </b-form-checkbox>
          <b-form-checkbox v-model="filters.topSkillTag" inline>
            <b-badge variant="info" class="ml-2">Top Skill</b-badge>
          </b-form-checkbox>
          <b-form-checkbox v-model="filters.highActivityTag" inline>
            <b-badge variant="success" class="ml-2">High Activity</b-badge>
          </b-form-checkbox>
          <b-form-checkbox v-model="filters.neverAchieved" inline>
            <b-badge variant="warning" class="ml-2">Never Achieved</b-badge>
          </b-form-checkbox>
          <b-form-checkbox v-model="filters.neverReported" inline>
            <b-badge variant="warning" class="ml-2">Never Reported</b-badge>
          </b-form-checkbox>
          <div class="text-muted small">Please Note: Tags become more meaningful with extensive usage</div>
        </b-form-group>
      </div>
    </div>
    <div class="row pl-3 mb-3">
      <div class="col">
        <b-button variant="outline-info" @click="applyFilters" data-cy="skillsNavigator-filterBtn"><i class="fa fa-filter"/> Filter</b-button>
        <b-button variant="outline-info" @click="reset" class="ml-1" data-cy="skillsNavigator-resetBtn"><i class="fa fa-times"/> Reset</b-button>
      </div>
    </div>

    <skills-b-table :items="items" :options="tableOptions" data-cy="skillsNavigator-table">
      <template v-slot:cell(skillName)="data">
        <span class="ml-2">{{ data.value }}</span>

        <b-button-group class="float-right">
          <b-button target="_blank" :to="{ name: 'SkillOverview', params: { projectId: projectId, subjectId: 'subj1', skillId: 'skill1' } }"
                    variant="outline-info" size="sm" class="text-secondary"
                    v-b-tooltip.hover="'View Skill Configuration'"><i class="fa fa-wrench"/></b-button>
          <b-button id="b-skill-metrics" target="_blank" :to="{ name: 'SkillMetrics', params: { projectId: projectId, subjectId: 'subj1', skillId: 'skill1' } }"
                    variant="outline-info" size="sm" class="text-secondary"
                    v-b-tooltip.hover="'View Skill Metrics'"><i class="fa fa-chart-bar"/></b-button>
        </b-button-group>
      </template>

      <template v-slot:cell(numUserAchieved)="data">
        <span class="ml-2">{{ data.value }}</span>
        <b-badge v-if="data.item.isOverlookedTag" variant="danger" class="ml-2">Overlooked Skill</b-badge>
        <b-badge v-if="data.item.isTopSkillTag" variant="info" class="ml-2">Top Skill</b-badge>
      </template>

      <template v-slot:cell(numUsersInProgress)="data">
        <span class="ml-2">{{ data.value }}</span>
        <b-badge v-if="data.item.isHighActivityTag" variant="success" class="ml-2">High Activity</b-badge>
      </template>

      <template v-slot:cell(lastAchievedTimestamp)="data">
        <b-badge v-if="data.item.isNeverAchievedTag" variant="warning" class="ml-2">Never</b-badge>
        <div v-else>
          <div>
            {{ data.value | date }}
          </div>
          <div class="text-muted small">
            <span>{{ relativeTime(data.value) }}</span>
          </div>
        </div>
      </template>

      <template v-slot:cell(lastReportedTimestamp)="data">
        <b-badge v-if="data.item.isNeverReportedTag" variant="warning" class="ml-2">Never</b-badge>
        <div v-else>
          <div>
            <span>{{ data.value | date }}</span>
          </div>
          <div class="text-muted small">
            {{ relativeTime(data.value) }}
          </div>
        </div>
      </template>
    </skills-b-table>
  </metrics-card>
</template>

<script>
  import moment from 'moment';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import MetricsService from '../MetricsService';
  import MetricsCard from '../utils/MetricsCard';

  export default {
    name: 'SkillsUsageMetrics',
    components: { MetricsCard, SkillsBTable },
    data() {
      return {
        projectId: this.$route.params.projectId,
        filters: {
          name: '',
          highActivityTag: false,
          overlookedTag: false,
          topSkillTag: false,
          neverAchieved: false,
          neverReported: false,
        },
        tableOptions: {
          busy: false,
          sortBy: 'timestamp',
          sortDesc: true,
          bordered: true,
          outlined: true,
          rowDetailsControls: false,
          stacked: 'md',
          fields: [
            {
              key: 'skillName',
              label: 'Skill',
              sortable: true,
            },
            {
              key: 'numUserAchieved',
              sortable: true,
              label: '# Users Achieved',
            },
            {
              key: 'numUsersInProgress',
              sortable: true,
              label: '# Users In Progress',
            },
            {
              key: 'lastReportedTimestamp',
              sortable: true,
              label: 'Last Reported',
            },
            {
              key: 'lastAchievedTimestamp',
              sortable: true,
              label: 'Last Achieved',
            },
          ],
          pagination: {
            currentPage: 1,
            totalRows: 1,
            pageSize: 5,
            possiblePageSizes: [5, 10, 15, 20, 50],
          },
        },
        items: [],
        originalItems: [],
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      applyFilters() {
        this.items = this.originalItems.filter((item) => {
          if (this.filters.name && !item.skillName.toLowerCase().includes(this.filters.name.toLowerCase())) {
            return false;
          }
          if (this.filters.neverAchieved && !item.isNeverAchievedTag) {
            return false;
          }
          if (this.filters.neverReported && !item.isNeverReportedTag) {
            return false;
          }
          if (this.filters.topSkillTag && !item.isTopSkillTag) {
            return false;
          }
          if (this.filters.overlookedTag && !item.isOverlookedTag) {
            return false;
          }
          if (this.filters.highActivityTag && !item.isHighActivityTag) {
            return false;
          }
          return true;
        });
      },
      addTags(items) {
        const numInTopPercent = Math.trunc(items.length * 0.1);
        console.log(`numInTopPercent: ${numInTopPercent}`);
        // up-to number of items in top or bottom skill tags
        const adjustmentThreshold = Math.trunc(items.length * 0.2);
        const enabled = items.length > 15;

        const sortedByNumAchieved = items.map((item) => item.numUserAchieved).sort((a, b) => (a - b));
        const minNumUserAchievedForTopSkillTag = sortedByNumAchieved[items.length - numInTopPercent];
        const maxNumUserAchievedForOverlookedTag = sortedByNumAchieved[numInTopPercent];
        console.log(`numTopSkill: ${sortedByNumAchieved}`);
        console.log(`numTopSkill: ${minNumUserAchievedForTopSkillTag}`);

        const numTopSkill = sortedByNumAchieved.length - sortedByNumAchieved.indexOf(minNumUserAchievedForTopSkillTag);
        const topSkillsTabEnabled = numTopSkill < adjustmentThreshold;
        console.log(`numTopSkill: ${numTopSkill}`);
        console.log(`topSkillsTabEnabled: ${topSkillsTabEnabled}`);

        const numOverlookedSkills = sortedByNumAchieved.lastIndexOf(maxNumUserAchievedForOverlookedTag) + 1;
        const overlookedTagEnabled = numOverlookedSkills <= adjustmentThreshold;
        console.log(`numOverlookedSkills: ${numOverlookedSkills}`);
        console.log(`overlookedTagEnabled: ${overlookedTagEnabled}`);

        const sortedByNumInProgress = items.map((item) => item.numUsersInProgress).sort((a, b) => (a - b));
        const minNumUserProgressForHighActivityTag = sortedByNumInProgress[items.length - numInTopPercent];
        const enabledHighActivity = minNumUserProgressForHighActivityTag > 0 && (minNumUserProgressForHighActivityTag - 10 > sortedByNumInProgress[0]);
        console.log(`sortedByNumInProgress: ${sortedByNumInProgress}`);
        console.log(`minNumUserProgressForHighActivityTag: ${minNumUserProgressForHighActivityTag}`);

        return items.map((item) => ({
          isTopSkillTag: (enabled && topSkillsTabEnabled && minNumUserAchievedForTopSkillTag <= item.numUserAchieved),
          isNeverAchievedTag: !item.lastAchievedTimestamp,
          isNeverReportedTag: !item.lastReportedTimestamp,
          isOverlookedTag: overlookedTagEnabled && enabled && item.numUserAchieved <= maxNumUserAchievedForOverlookedTag,
          isHighActivityTag: enabled && enabledHighActivity && (item.numUsersInProgress >= minNumUserProgressForHighActivityTag),
          ...item,
        }));
      },
      reset() {
        this.filters.name = '';
        this.filters.neverAchieved = false;
        this.filters.neverReported = false;
        this.filters.overlookedTag = false;
        this.filters.topSkillTag = false;
        this.filters.highActivityTag = false;
        this.items = this.originalItems;
      },
      loadData() {
        this.tableOptions.busy = true;
        MetricsService.loadChart(this.$route.params.projectId, 'skillUsageNavigatorChartBuilder')
          .then((dataFromServer) => {
            this.items = this.addTags(dataFromServer);
            // console.log(`items: ${JSON.stringify(this.items)}`);
            this.originalItems = this.items;
            this.tableOptions.pagination.totalRows = this.items.length;
            this.tableOptions.busy = false;
          });
      },
      relativeTime(timestamp) {
        return moment(timestamp)
          .startOf('hour')
          .fromNow();
      },
    },
  };
</script>

<style scoped>

</style>
