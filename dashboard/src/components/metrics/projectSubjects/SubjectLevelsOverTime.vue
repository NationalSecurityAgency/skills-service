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
<div class="card mt-2"  data-cy="subjectNumUsersPerLevelOverTime">
  <div class="card-header">
    <h5>Number of users for each level over time</h5>
  </div>
  <div class="card-body">
    <b-form inline class="mb-4">
      <b-overlay :show="loading.subjects" rounded="sm" opacity="0.5"
                 spinner-variant="info" spinner-type="grow" spinner-small>
        <b-form-select v-model="subjects.selected" :options="subjects.available"
                       data-cy="subjectNumUsersPerLevelOverTime-subjectSelector" required>
          <template v-slot:first>
            <b-form-select-option :value="null" disabled>-- Please select a subject --</b-form-select-option>
          </template>
        </b-form-select>
      </b-overlay>
      <b-button variant="outline-info" class="ml-2" :disabled="!subjects.selected"
        @click="loadChart">
        <i class="fas fa-paint-roller"></i> Generate
      </b-button>
    </b-form>
    <b-overlay :show="loading.charts || isSeriesEmpty" opacity=".5">
      <apexchart type="area" height="300" :options="chartOptions" :series="series"></apexchart>
      <template v-slot:overlay>
        <div v-if="loading.charts">
          <b-spinner variant="info" label="Spinning"></b-spinner>
        </div>
        <div v-if="!loading.charts && !loading.generatedAtLeastOnce && isSeriesEmpty" class="alert alert-info">
          <i class="fas fa-chart-line"></i> Generate the chart using controls above!
        </div>
        <div v-if="!loading.charts && loading.generatedAtLeastOnce && isSeriesEmpty" class="alert alert-info">
          <i class="fas fa-cat"></i> Zero users achieved levels for this subject!
        </div>
      </template>
    </b-overlay>
  </div>
</div>
</template>

<script>
  import numberFormatter from '@/filters/NumberFilter';
  import MetricsService from '../MetricsService';
  import SubjectsService from '../../subjects/SubjectsService';

  export default {
    name: 'SubjectLevelsOverTime',
    data() {
      return {
        loading: {
          subjects: true,
          charts: false,
          generatedAtLeastOnce: false,
        },
        subjects: {
          selected: null,
          available: [],
        },
        series: [],
        chartOptions: {
          chart: {
            type: 'line',
            toolbar: {
              offsetY: -20,
            },
          },
          colors: ['#008FFB', '#546E7A', '#00E396'],
          yaxis: {
            title: {
              text: '# of users',
            },
            labels: {
              formatter(val) {
                return numberFormatter(val);
              },
            },
          },
          xaxis: {
            type: 'datetime',
          },
          dataLabels: {
            enabled: false,
          },
          legend: {
            showForSingleSeries: true,
          },
        },
      };
    },
    mounted() {
      this.loadSubjects();
      // this.loadChart('subj1');
    },
    computed: {
      isSeriesEmpty() {
        return !this.series || this.series.length === 0;
      },
    },
    methods: {
      loadSubjects() {
        SubjectsService.getSubjects(this.$route.params.projectId)
          .then((res) => {
            this.subjects.available = res.map((subj) => ({ value: subj.subjectId, text: subj.name }));
            this.loading.subjects = false;
          });
      },
      loadChart() {
        this.loading.charts = true;
        const params = { subjectId: this.subjects.selected };
        MetricsService.loadChart(this.$route.params.projectId, 'usersByLevelForSubjectOverTimeChartBuilder', params)
          .then((res) => {
            this.series = res.map((resItem) => {
              const data = resItem.counts.map((dayCount) => [dayCount.value, dayCount.count]);
              return {
                name: `Level ${resItem.level}`,
                data,
              };
            });

            this.loading.charts = false;
            this.loading.generatedAtLeastOnce = true;
          });
      },
    },

  };
</script>

<style scoped>

</style>
