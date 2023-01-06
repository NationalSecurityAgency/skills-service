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
    <div class="p-4">
      <div class="h3">Question #{{num + 1}}</div>
      <div>
        <markdown-text :text="q.question"/>
      </div>
      <div class="row no-gutters">
        <div class="col-auto">
            <apexchart :type="chartOptions.chart.type" :width="chartOptions.chart.width" :options="chartOptions" :series="series"></apexchart>
        </div>
        <div class="col">
        </div>
      </div>
    </div>
    <skills-b-table v-if="answers" :options="tableOptions" :items="answers">
      <template #head(answer)="data">
        <span class="text-primary"><i class="fas fa-check-double skills-color-projects" aria-hidden="true"></i> {{ data.label }}</span>
      </template>
      <template #head(isCorrect)="data">
        <span class="text-primary"><i class="fas fa-thumbs-up skills-color-events" aria-hidden="true"></i> {{ data.label }}</span>
      </template>
      <template #head(numAnswered)="data">
        <span class="text-primary"><i class="fas fa-user-check skills-color-badges" aria-hidden="true"></i> {{ data.label }}</span>
      </template>
      <template v-slot:cell(answer)="data">
        <check-selector :value="data.item.isCorrect" :read-only="true" font-size="1.5rem"/> {{ data.value }}
      </template>
      <template v-slot:cell(isCorrect)="data">
        <check-selector v-if="data.value" :value="data.value" :read-only="true"/>
      </template>
      <template v-slot:cell(numAnswered)="data">
        {{ data.value }} <b-badge>{{ data.item.percent }}%</b-badge>
      </template>
    </skills-b-table>

  </div>
</template>

<script>
  import MarkdownText from '@/common-components/utilities/MarkdownText';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import CheckSelector from '@/common-components/quiz/CheckSelector';

  export default {
    name: 'QuizQuestionMetrics',
    components: { CheckSelector, SkillsBTable, MarkdownText },
    props: {
      q: Object,
      num: Number,
    },
    data() {
      return {
        series: [this.q.numAnsweredCorrect, this.q.numAnsweredWrong],
        chartOptions: {
          labels: ['Correct', 'Wrong'],
          colors: ['#007c49', '#ffc42b'],
          chart: {
            width: 300,
            type: 'donut',
          },
          plotOptions: {
            pie: {
              startAngle: -180,
              endAngle: 180,
            },
          },
          dataLabels: {
            enabled: true,
            style: {
              fontSize: '14px',
            },
            background: {
              enabled: true,
              foreColor: '#146c75',
              padding: 6,
              borderRadius: 2,
              borderWidth: 1,
              borderColor: '#3cbcad',
              opacity: 1,
              dropShadow: {
                enabled: false,
                top: 1,
                left: 1,
                blur: 1,
                color: '#000',
                opacity: 0.45,
              },
            },
            dropShadow: {
              enabled: false,
            },
          },
          fill: {
            type: 'gradient',
          },
          legend: {
            position: 'left',
            formatter(val, opts) {
              return `${val}: ${opts.w.globals.series[opts.seriesIndex]} Users`;
            },
          },
        },
        answers: null,
        tableOptions: {
          bordered: true,
          outlined: true,
          stacked: 'md',
          sortBy: 'userId',
          sortDesc: true,
          fields: [
            {
              key: 'answer',
              label: 'Answer',
              sortable: true,
            },
            {
              key: 'numAnswered',
              label: '# Users Selected',
              sortable: true,
            },
          ],
          pagination: {
            hideUnnecessary: true,
            server: false,
            currentPage: 1,
            totalRows: 1,
            pageSize: 5,
            possiblePageSizes: [5, 10, 15, 20],
          },
        },
      };
    },
    mounted() {
      const totalNumUsers = this.q.answers.map((a) => a.numAnswered).reduce((a, b) => a + b);
      this.answers = this.q.answers.map((a) => ({ ...a, selected: a.selected ? a.selected : false, percent: Math.trunc((a.numAnswered / totalNumUsers) * 100) }));
    },
  };
</script>

<style scoped>

</style>
