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
  <div :data-cy="`metrics-q${qNum}`">
    <div class="p-4">
      <div class="h3">Question #{{qNum}} <span class="h5"><b-badge variant="info" data-cy="qType">{{ questionTypeLabel }}</b-badge></span></div>
      <div>
        <markdown-text :text="q.question"/>
      </div>
      <div v-if="!isSurvey" class="row no-gutters">
        <div class="col-auto">
            <apexchart :type="chartOptions.chart.type" :width="chartOptions.chart.width" :options="chartOptions" :series="series"></apexchart>
        </div>
        <div class="col">
        </div>
      </div>
    </div>

    <div v-if="isRating && averageScore" class="pl-3">
      Average Score:
      <b-form-rating no-border readonly inline :value="averageScore" size="lg"
                     variant="warning" show-value precision="2" :stars="numberOfStars" />
    </div>

    <skills-b-table v-if="!isTextInput && answers"
                    :options="tableOptions"
                    :items="answers">
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
        <div :data-cy="`row${data.index}-colAnswer`">
          <check-selector v-if="!isSurvey" :value="data.item.isCorrect" :read-only="true" font-size="1.5rem" :data-cy="`checkbox-${data.item.isCorrect}`"/> {{ data.value }}
        </div>
      </template>
      <template v-slot:cell(isCorrect)="data">
        <check-selector v-if="data.value" :value="data.value" :read-only="true"/>
      </template>
      <template v-slot:cell(numAnswered)="data">
        <div class="row" :data-cy="`row${data.index}-colNumAnswered`">
          <div class="col">
            <span data-cy="num">{{ data.value }}</span> <b-badge data-cy="percent">{{ data.item.percent }}%</b-badge>
          </div>
          <div class="col-auto" v-if="data.item.numAnswered && data.item.numAnswered > 0">
            <b-button size="sm" variant="outline-primary" @click="data.toggleDetails" data-cy="answerHistoryBtn">
              <i :class="{'fa-arrow-alt-circle-up' : data.detailsShowing, 'fa-arrow-alt-circle-down' : !data.detailsShowing }" class="fas" />  Answer's History
            </b-button>
          </div>
        </div>
      </template>

      <template #row-details="row">
        <quiz-answer-history :answer-def-id="row.item.id"
                             :data-cy="`row${row.index}-answerHistory`"
                             class="mb-4"/>
      </template>
    </skills-b-table>
    <div v-if="!isSurvey && isMultipleChoice" class="bg-light p-2 small" data-cy="multipleChoiceQuestionWarning">
      *** All of the required choices must be selected for the question to be counted as <span class="text-success text-uppercase">correct</span> ***
    </div>

    <quiz-answer-history v-if="isSurvey && isTextInput"
                         :question-type="q.questionType"
                         :answer-def-id="q.answers[0].id"/>

  </div>
</template>

<script>
  import MarkdownText from '@/common-components/utilities/MarkdownText';
  import CheckSelector from '@/common-components/quiz/CheckSelector';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import QuizAnswerHistory from '@/components/quiz/metrics/QuizAnswerHistory';

  export default {
    name: 'QuizQuestionMetrics',
    components: {
      QuizAnswerHistory, CheckSelector, SkillsBTable, MarkdownText,
    },
    props: {
      q: Object,
      isSurvey: Boolean,
      num: Number,
    },
    data() {
      return {
        series: [this.q.numAnsweredCorrect, this.q.numAnsweredWrong],
        averageScore: null,
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
              return `${val}: ${opts.w.globals.series[opts.seriesIndex]} Attempts`;
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
              sortable: false,
            },
            {
              key: 'numAnswered',
              label: '# of Times Selected',
              sortable: false,
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
      const totalNumUsers = this.q.numAnsweredCorrect + this.q.numAnsweredWrong;
      this.answers = this.q.answers.map((a) => ({ ...a, selected: a.selected ? a.selected : false, percent: (totalNumUsers > 0 ? Math.trunc((a.numAnswered / totalNumUsers) * 100) : 0) }));
      if (this.isRating) {
        let totalScore = 0;
        let totalAnswers = 0;
        this.answers.forEach((answer) => {
          totalAnswers += answer.numAnswered;
          totalScore += (answer.answer * answer.numAnswered);
        });
        this.averageScore = totalScore / totalAnswers;
      }
    },
    computed: {
      questionTypeLabel() {
        return this.q.questionType.match(/[A-Z][a-z]+/g).join(' ');
      },
      isMultipleChoice() {
        return this.q.questionType === 'MultipleChoice';
      },
      isTextInput() {
        return this.q.questionType === 'TextInput';
      },
      isRating() {
        return this.q.questionType === 'Rating';
      },
      qNum() {
        return this.num + 1;
      },
      numberOfStars() {
        return this.q.answers.length;
      },
    },
  };
</script>

<style scoped>

</style>
