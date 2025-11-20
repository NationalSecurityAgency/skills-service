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
import {useColors} from "@/skills-display/components/utilities/UseColors.js";
import Chart from "primevue/chart";
import ChartDataLabels from 'chartjs-plugin-datalabels';
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";
import {usePluralize} from "@/components/utils/misc/UsePluralize.js";

const props = defineProps({
  q: Object,
  isSurvey: Boolean,
  num: Number,
  dateRange: Array,
})

const responsive = useResponsiveBreakpoints()
const colors = useColors()
const chartSupportColors = useChartSupportColors()
const pluralize = usePluralize()

const averageScore = ref(0)
const answers = ref([])
const expandedRows = ref([])

const chartJsOptions = ref(null)

const chartColors = chartSupportColors.getColors()
const chartData = computed(() => {
  return {
    labels: [`Correct: ${props.q.numAnsweredCorrect} Attempts`, `Wrong: ${props.q.numAnsweredWrong} Attempts`],
    datasets: [
      {
        label: 'Attempts',
        data: [props.q.numAnsweredCorrect, props.q.numAnsweredWrong],
        backgroundColor: [chartColors.green700Color, chartColors.orange700Color],
        datalabels: {
          anchor: 'end'
        }
      }
    ]
  }
})
const overallCorrectPercent = computed(() => {
  const total = props.q.numAnsweredCorrect + props.q.numAnsweredWrong
  if (total === props.q.numAnsweredCorrect) {
    return 100
  }
  return total > 0 ? (props.q.numAnsweredCorrect / total * 100).toFixed(1) : 0
})
const overallCorrectWrong = computed(() => 100 - overallCorrectPercent.value)
const setChartOptions = () => {
  return {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '60%',
    layout: {
      padding: {
        left: 20,  // Add left padding to prevent cutting off
        top: 20,
        bottom: 20,
      }
    },
    hover: { mode: null },
    plugins: {
      legend: {
        display: false,
      },
      title: {
        display: false,
      },
      tooltip: {
        enabled: false,
      },
      datalabels: {
        font: {
          weight: 'bold'
        },
        backgroundColor: function(context) {
          return context.dataset.backgroundColor;
        },
        borderColor: 'white',
        borderRadius: 25,
        borderWidth: 2,
        color: 'white',
        padding: 6,
        formatter: function(value, context) {
          const percent = context.dataIndex === 0 ? overallCorrectPercent.value : overallCorrectWrong.value
          return `${percent}%`
        }
      }
    },
  };
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
    imageClass: `fas fa-check-double ${colors.getTextClass(0)}`
  })
  tableFields.push({
    key: 'numAnswered',
    label: '# of Times Selected',
    sortable: false,
    imageClass: `fas fa-user-check ${colors.getTextClass(1)}`
  })
}
else {
  tableFields.push({
    key: 'multiPartAnswer',
    label: 'Answer',
    sortable: false,
    imageClass: `fas fa-check-double ${colors.getTextClass(0)}`
  })
  tableFields.push({
    key: 'numAnsweredCorrect',
    label: '# Correct Matches',
    sortable: false,
    imageClass: `fas fa-user-check ${colors.getTextClass(1)}`
  })
  tableFields.push({
    key: 'numAnsweredWrong',
    label: '# Incorrect Matches',
    sortable: false,
    imageClass: `fas fa-circle-xmark ${colors.getTextClass(2)}`
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
  chartJsOptions.value = setChartOptions()
  const totalNumUsers = props.q.numAnsweredCorrect + props.q.numAnsweredWrong
  answers.value = props.q.answers.map((a) => ({
    ...a,
    selected: a.selected ? a.selected : false,
    percent: (totalNumUsers > 0 ? Math.trunc(((isMatching.value ? a.numAnsweredCorrect : a.numAnswered) / totalNumUsers) * 100) : 0),
    percentWrong: (totalNumUsers > 0 ? Math.trunc(((isMatching.value ? a.numAnsweredWrong : a.numAnswered) / totalNumUsers) * 100) : 0),
    multiPartAnswer: a.multiPartAnswer ? a.multiPartAnswer : null
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
          <div class="flex flex-col lg:flex-row items-center gap-3" data-cy="qMetrics">
            <Chart type="doughnut"
                   :data="chartData"
                   :options="chartJsOptions"
                   :plugins="[ChartDataLabels]"
                   class="h-[15rem]"
            />
            <div class="flex gap-2 items-center">
              <div class="flex flex-col gap-2">
                <div class="flex gap-1 items-center">
                  <div class="w-4 h-4 rounded-sm border border-white" :style="{ backgroundColor: chartColors.green700Color }"></div>
                  <span>Correct: </span>
                </div>
                <div class="flex gap-1 items-center">
                  <div class="w-4 h-4 rounded-sm border border-white" :style="{ backgroundColor: chartColors.orange700Color }"></div>
                  <span>Wrong: </span>
                </div>
              </div>
              <div  class="flex flex-col gap-2">
                <div><Tag data-cy="numCorrect">{{ q.numAnsweredCorrect }}</Tag> {{ pluralize.plural('Attempt', q.numAnsweredCorrect) }} <span class="text-surface-600 dark:text-white" data-cy="percentCorrect">({{ overallCorrectPercent}}%)</span> </div>
                <div><Tag severity="warn" data-cy="numWrong">{{ q.numAnsweredWrong }}</Tag> {{ pluralize.plural('Attempt', q.numAnsweredWrong) }} <span class="text-surface-600 dark:text-white" data-cy="percentWrong">({{ overallCorrectWrong}}%)</span></div>
              </div>
            </div>
          </div>
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
      :expander="!isMatching"
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
            <Tag data-cy="percent" :severity="(isSurvey || slotProps.data.isCorrect) ? 'success' : 'warn'">{{ slotProps.data.percent }}%</Tag>
          </div>
          <div v-else-if="slotProps.field === 'numAnsweredWrong'" :data-cy="`row${slotProps.index}-colNumAnsweredWrong`">
            <span data-cy="num" class="pr-1">{{ slotProps.data[col.key] }}</span>
            <Tag v-if="slotProps.data.percent < 100" data-cy="percent" severity="warn">{{ slotProps.data.percentWrong }}%</Tag>
          </div>
          <div v-else-if="slotProps.field === 'multiPartAnswer'" :data-cy="`row${slotProps.index}-multiPartAnswerCol`">
            {{ slotProps.data[col.key].term }}: <span class="font-semibold">{{ slotProps.data[col.key].value }}</span>
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

    <div v-if="!isSurvey && isMatching" class="bg-surface-100 dark:bg-surface-700 p-2 text-sm" data-cy="matchingQuestionWarning">
      *** All matches must be correct for the question to be counted as <span class="text-primary uppercase">correct</span> ***
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