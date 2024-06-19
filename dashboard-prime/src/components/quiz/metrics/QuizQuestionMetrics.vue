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

import { computed, onMounted, ref } from 'vue'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import Column from 'primevue/column'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'
import CheckSelector from '@/skills-display/components/quiz/CheckSelector.vue'
import QuizAnswerHistory from '@/components/quiz/metrics/QuizAnswerHistory.vue'

const props = defineProps({
  q: Object,
  isSurvey: Boolean,
  num: Number
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
const tableOptions = {
  bordered: true,
  outlined: true,
  stacked: 'md',
  fields: [
    {
      key: 'answer',
      label: 'Answer',
      sortable: false,
      imageClass: 'fas fa-check-double skills-color-projects'
    },
    {
      key: 'numAnswered',
      label: '# of Times Selected',
      sortable: false,
      imageClass: 'fas fa-user-check skills-color-badges'
    }
  ],
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
    percent: (totalNumUsers > 0 ? Math.trunc((a.numAnswered / totalNumUsers) * 100) : 0)
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
const isMultipleChoice = computed(() => {
  return props.q.questionType === 'MultipleChoice'
})
const isTextInput = computed(() => {
  return props.q.questionType === 'TextInput'
})
const isRating = computed(() => {
  return props.q.questionType === 'Rating'
})
const qNum = computed(() => {
  return props.num + 1
})
const numberOfStars = computed(() => {
  return props.q.answers.length
})
</script>

<template>
  <div :data-cy="`metrics-q${qNum}`">
    <div class="p-4">
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
                     :series="series"></apexchart>
        </div>
      </div>
    </div>

    <div v-if="isRating && averageScore" class="flex align-items-baseline flex-wrap pl-3">
      Average Score:
      <Rating class="flex-initial border-round py-3 px-4" v-model="averageScore" :stars="numberOfStars" readonly
              :cancel="false" />
      <span class="text-lg">{{ averageScore }}</span>
    </div>
    <SkillsDataTable
      tableStoredStateId="quizQuestionMetrics"
      v-if="!isTextInput && answers"
      v-model:expandedRows="expandedRows"
      :expander="true"
      expander-label="Expand Answer History"
      :expander-pt="{
                rowToggler: ({ instance: { rowData } }) => ({
                  class: {
                    hidden: !(rowData?.numAnswered && rowData.numAnswered > 0),
                  },
                }),
              }"
      :value="answers">
      <Column v-for="col of tableOptions.fields" :key="col.key" :field="col.key" :sortable="col.sortable"
              :class="{'flex': responsive.md.value }">
        <template #header>
          <span><i :class="col.imageClass" aria-hidden="true"></i> {{ col.label }}</span>
        </template>
        <template #body="slotProps">
          <div v-if="slotProps.field === 'answer'" :data-cy="`row${slotProps.index}-colAnswer`">
            <CheckSelector v-if="!isSurvey" :value="slotProps.data.isCorrect" :read-only="true" font-size="1.5rem"
                           :data-cy="`checkbox-${slotProps.data.isCorrect}`" />
            {{ slotProps.data[col.key] }}
          </div>
          <div v-else-if="slotProps.field === 'numAnswered'" :data-cy="`row${slotProps.index}-colNumAnswered`">
            <span data-cy="num" class="pr-1">{{ slotProps.data[col.key] }}</span>
            <Tag data-cy="percent">{{ slotProps.data.percent }}%</Tag>
          </div>
          <div v-else>
            {{ slotProps.data[col.key] }}
          </div>
        </template>
      </Column>
      <template #expansion="slotProps">
        <QuizAnswerHistory :answer-def-id="slotProps.data.id"
                           :data-cy="`row${slotProps.index}-answerHistory`"
                           class="mb-4" />
      </template>
    </SkillsDataTable>

    <div v-if="!isSurvey && isMultipleChoice" class="bg-gray-100 p-2 text-sm" data-cy="multipleChoiceQuestionWarning">
      *** All of the required choices must be selected for the question to be counted as <span
      class="text-success uppercase">correct</span> ***
    </div>

    <QuizAnswerHistory v-if="isSurvey && isTextInput"
                       :question-type="q.questionType"
                       :answer-def-id="q.answers[0].id" />

  </div>

</template>

<style scoped>

</style>