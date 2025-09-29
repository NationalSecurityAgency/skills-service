/*
Copyright 2024 SkillTree

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
<script setup>

import { computed, onMounted, ref, watch } from 'vue'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import Column from 'primevue/column'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'
import CheckSelector from '@/skills-display/components/quiz/CheckSelector.vue'
import QuizAnswerHistory from '@/components/quiz/metrics/QuizAnswerHistory.vue'

const props = defineProps({
  q: Object,
  isSurvey: Boolean,
  num: Number,
  dateRange: Array,
})

const responsive = useResponsiveBreakpoints()

const averageScore = ref(0)
const answers = ref([])
const expandedRows = ref([])

const series = [props.q.numAnsweredCorrect, props.q.numAnsweredWrong]
const chartOptions = {
  labels: ['Correct', 'Wrong'],
  colors: ['#007c49', '#ffc42b'],
  chart: {
    width: 300,
    type: 'donut'
  },
  plotOptions: {
    pie: {
      startAngle: -180,
      endAngle: 180
    }
  },
  dataLabels: {
    enabled: true,
    style: {
      fontSize: '14px'
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
        opacity: 0.45
      }
    },
    dropShadow: {
      enabled: false
    }
  },
  fill: {
    type: 'gradient'
  },
  legend: {
    position: 'left',
    formatter(val, opts) {
      return `${val}: ${opts.w.globals.series[opts.seriesIndex]} Attempts`
    }
  }
}

const isMultipleChoice = computed(() => {
  return props.q.questionType === 'MultipleChoice'
})
const isTextInput = computed(() => {
  return props.q.questionType === 'TextInput'
})
const isMatching = computed(() => {
  return props.q.questionType === 'Matching'
})
const isRating = computed(() => {
  return props.q.questionType === 'Rating'
})

const questionCorrectChartRef = ref()
watch(() => responsive.sm.value, (newValue) => {
  if (!props.isSurvey) {
    questionCorrectChartRef.value.updateOptions({
      legend: {
        position: newValue ? 'bottom' : 'left',
      },
      chart: {
        width: newValue ? 250 : 300,
      },
    })
  }
})

const tableFields = []

if(!isMatching.value) {
  tableFields.push({
    key: 'answer',
    label: 'Answer',
    sortable: false,
    imageClass: 'fas fa-check-double skills-color-projects'
  })
  tableFields.push({
    key: 'numAnswered',
    label: '# of Times Selected',
    sortable: false,
    imageClass: 'fas fa-user-check skills-color-badges'
  })
}
else {
  tableFields.push({
    key: 'multiPartAnswer',
    label: 'Answer',
    sortable: false,
    imageClass: 'fas fa-check-double skills-color-projects'
  })
  tableFields.push({
    key: 'numAnsweredCorrect',
    label: '# of Times Correctly Matched',
    sortable: false,
    imageClass: 'fas fa-user-check skills-color-badges'
  })
}

const tableOptions = {
  bordered: true,
  outlined: true,
  stacked: 'md',
  fields: tableFields,
  pagination: {
    hideUnnecessary: true,
    server: false,
    currentPage: 1,
    totalRows: props.q.answers.length,
    pageSize: 5,
    possiblePageSizes: [5, 10, 15, 20]
  }
}

onMounted(() => {
  const totalNumUsers = props.q.numAnsweredCorrect + props.q.numAnsweredWrong
  answers.value = props.q.answers.map((a) => ({
    ...a,
    selected: a.selected ? a.selected : false,
    percent: (totalNumUsers > 0 ? Math.trunc(((isMatching.value ? a.numAnsweredCorrect : a.numAnswered) / totalNumUsers) * 100) : 0),
    multiPartAnswer: a.multiPartAnswer?.length > 0 ? JSON.parse(a.multiPartAnswer) : null
  }))
  if (isRating.value) {
    let totalScore = 0
    let totalAnswers = 0
    answers.value.forEach((answer) => {
      totalAnswers += answer.numAnswered
      totalScore += (answer.answer * answer.numAnswered)
    })
    averageScore.value = totalScore / totalAnswers
  }
})

const questionTypeLabel = computed(() => {
  return props.q.questionType.match(/[A-Z][a-z]+/g).join(' ')
})
const qNum = computed(() => {
  return props.num + 1
})
const numberOfStars = computed(() => {
  return props.q.answers.length
})
const removeExpanderClass = (rowData) => {
  if (!(rowData?.numAnswered && rowData.numAnswered > 0) ){
    return 'no-expander'
  }
  return ''
}
</script>

<template>
  <div :data-cy="`metrics-q${qNum}`">
    <div class="p-6">
      <div class="text-3xl">Question #{{ qNum }}
        <Tag class="text-lg" severity="info" data-cy="qType">{{ questionTypeLabel }}</Tag>
      </div>
      <div>
        <markdown-text :text="q.question"
                       :instance-id="`${q.id}`" />
      </div>
      <div v-if="!isSurvey">
        <div>
          <apexchart :type="chartOptions.chart.type" :width="chartOptions.chart.width" :options="chartOptions"
                     ref="questionCorrectChartRef"
                     :series="series"></apexchart>
        </div>
      </div>
    </div>

    <div v-if="isRating && averageScore" class="flex items-baseline flex-wrap pl-4">
      Average Score:
      <Rating class="flex-initial rounded-border py-4 px-6" v-model="averageScore" :stars="numberOfStars" readonly
              :cancel="false" />
      <span class="text-lg">{{ averageScore }}</span>
    </div>

    <SkillsDataTable
      tableStoredStateId="quizQuestionMetrics"
      aria-label="Question Metrics"
      v-if="!isTextInput && answers"
      v-model:expandedRows="expandedRows"
      :expander="isMatching ? false : true"
      expander-label="Expand Answer History"
      :row-class="removeExpanderClass"
      :value="answers">
      <Column v-for="col of tableOptions.fields" :key="col.key" :field="col.key" :sortable="col.sortable"
              :class="{'flex': responsive.md.value }">
        <template #header>
          <span><i :class="col.imageClass" aria-hidden="true"></i> {{ col.label }}</span>
        </template>
        <template #body="slotProps">
          <div v-if="slotProps.field === 'answer'" :data-cy="`row${slotProps.index}-colAnswer`">
            <CheckSelector v-if="!isSurvey" v-model="slotProps.data.isCorrect" :read-only="true" font-size="1.5rem"
                           :data-cy="`checkbox-${slotProps.data.isCorrect}`" />
            {{ slotProps.data[col.key] }}
          </div>
          <div v-else-if="slotProps.field === 'numAnswered' || slotProps.field === 'numAnsweredCorrect'" :data-cy="`row${slotProps.index}-colNumAnswered`">
            <span data-cy="num" class="pr-1">{{ slotProps.data[col.key] }}</span>
            <Tag data-cy="percent">{{ slotProps.data.percent }}%</Tag>
          </div>
          <div v-else-if="slotProps.field === 'multiPartAnswer'">
            {{ slotProps.data[col.key].term }}: {{ slotProps.data[col.key].value }}
          </div>
          <div v-else>
            {{ slotProps.data[col.key] }}
          </div>
        </template>
      </Column>
      <template #expansion="slotProps">
        <QuizAnswerHistory :answer-def-id="slotProps.data.id"
                           :is-survey="isSurvey"
                           :data-cy="`row${slotProps.index}-answerHistory`"
                           :dateRange="dateRange"
                           class="mb-6" />
      </template>
    </SkillsDataTable>

    <div v-if="!isSurvey && isMultipleChoice" class="bg-surface-100 dark:bg-surface-700 p-2 text-sm" data-cy="multipleChoiceQuestionWarning">
      *** All of the required choices must be selected for the question to be counted as <span
      class="text-primary uppercase">correct</span> ***
    </div>

    <QuizAnswerHistory v-if="isTextInput || isMatching"
                       :is-survey="isSurvey"
                       :question-type="q.questionType"
                       :answer-def-id="q.answers[0].id" />

  </div>

</template>

<style scoped>

</style>
<style>
.p-datatable .p-datatable-tbody > tr.no-expander > td .p-datatable-row-toggle-button {
  display: none;
}
</style>