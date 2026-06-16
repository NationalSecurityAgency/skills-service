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
import {computed, inject, ref} from 'vue'
import {useFieldValue} from 'vee-validate'
import QuizSelector from '@/components/skills/selfReport/QuizSelector.vue'
import PendingApprovalsWarning from "@/components/skills/inputForm/PendingApprovalsWarning.vue";

const props = defineProps({
  initialSkillData: Object,
  isEdit: Boolean
})
const emit = defineEmits(['quizIdChanged', 'selfReportingTypeChanged'])

const approvalKey = 'Approval'
const honorKey = 'HonorSystem'
const quizKey = 'Quiz'
const videoKey = 'Video'

// const enabled = ref(props.initialSkillData.selfReportingEnabled)
const categories = ref([
  { name: 'Approval Queue', key: approvalKey },
  { name: 'Honor System', key: honorKey },
  { name: 'Quiz/Survey', key: quizKey },
  { name: 'Audio/Video', key: videoKey }
])
const setFieldValue = inject('setFieldValue')
const onEnabledChanged = (newVal) => {
  const selfReportingType = newVal ? approvalKey : ''
  setFieldValue('selfReportingType', selfReportingType)
  onTypeChanged(selfReportingType)
}
const onTypeChanged = (newType) => {
  if (newType !== approvalKey) {
    // justificationRequired.value = false
    setFieldValue('justificationRequired', false)
  }
  if (newType === quizKey) {
    setFieldValue('numPerformToCompletion', 1)
    setFieldValue('timeWindowEnabled', false)
    setFieldValue('numPointIncrementMaxOccurrences', 1)
  }
  emit('selfReportingTypeChanged', newType)
}
const quizIdSelected = (quizId) => {
  emit('quizIdChanged', quizId)
}

const selfReportingType = useFieldValue('selfReportingType')
const enabled = useFieldValue('selfReportingEnabled')
const quizSelected = computed(() => selfReportingType.value === quizKey)
const isVideoChoiceDisabled = computed(() => !enabled.value || !props.isEdit || (props.isEdit && !props.initialSkillData.hasVideoConfigured))
const isVideoConfigured = computed(() => props.initialSkillData.hasVideoConfigured)
</script>

<template>
  <div>
    <div class="flex flex-wrap md:flex-no-wrap">
      <!--        @input="onEnabledChanged"-->
      <div class="w-min-10rem mt-6 w-full md:w-auto">
        <SkillsCheckboxInput
          @update:modelValue="onEnabledChanged"
          :binary="true"
          inputId="selfReport"
          name="selfReportingEnabled"
          data-cy=selfReportEnableCheckbox />
        <label for="selfReport" class="ml-2"> Self Reporting </label>
      </div>

      <div class="card flex flex-1 sm:ml-6 mt-6">
        <div class="flex flex-col gap-4 w-full" data-cy="selfReportTypeSelector">
          <div v-for="category in categories" :key="category.key" class="flex items-start">
            <SkillsRadioButtonInput
              @update:modelValue="onTypeChanged"
              :disabled="!enabled || (category.key === videoKey && isVideoChoiceDisabled)"
              :inputId="category.key"
              severity="info"
              name="selfReportingType"
              :data-cy="`${category.key.toLowerCase()}Radio`"
              :value="category.key" />
            <label :for="category.key" class="ml-2 flex w-full">
              <span
                :class="{ 'text-color-secondary' : !enabled || (category.key === videoKey && isVideoChoiceDisabled) }">{{ category.name
                }}</span>
              <div v-if="category.key === approvalKey" class="ml-2">
                |
                <SkillsCheckboxInput
                  class="ml-2"
                  :binary="true"
                  :disabled="!enabled || selfReportingType !== 'Approval'"
                  inputId="selfReportJustificationRequired"
                  name="justificationRequired"
                  data-cy="justificationRequiredCheckbox"
                  :value="true" />
                <label
                  for="selfReportJustificationRequired"
                  :class="{ 'text-color-secondary' : !enabled || selfReportingType !== 'Approval' }"
                  class="ml-2 italic">Justification Required</label>
              </div>
              <div v-if="category.key === quizKey" class="pl-2 w-full">
                <QuizSelector class="mb-0" v-if="category.key === quizKey && enabled && quizSelected"
                              :initiallySelectedQuizId="initialSkillData.quizId" @changed="quizIdSelected" />
              </div>
              <span v-if="category.key === videoKey && !isVideoConfigured" class="ml-2 italic"
                    :class="{ 'text-color-secondary' : !enabled || (category.key === videoKey && isVideoChoiceDisabled) }"
                    data-cy="videoSelectionMsg">
                  <span v-if="!isEdit">(Please create skill and configure video settings first)</span>
                  <span v-else>(Please configure video settings first)</span>
                </span>
            </label>
          </div>
        </div>
      </div>
    </div>

    <pending-approvals-warning
        v-if="isEdit"
        :self-reporting-type="selfReportingType"
        :self-report-enabled="enabled"
        :skill-ids="[initialSkillData.skillId]"/>
  </div>
</template>

<style scoped>

</style>