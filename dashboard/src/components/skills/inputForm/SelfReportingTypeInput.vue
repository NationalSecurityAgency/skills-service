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
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useFieldValue } from 'vee-validate'
import QuizSelector from '@/components/skills/selfReport/QuizSelector.vue'
import SelfReportService from '@/components/skills/selfReport/SelfReportService.js'
import { useRoute } from 'vue-router'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'

const props = defineProps({
  initialSkillData: Object,
  isEdit: Boolean
})
const emit = defineEmits(['quizIdChanged', 'selfReportingTypeChanged'])

const route = useRoute()
const numFormat = useNumberFormat()

const approvalKey = 'Approval'
const honorKey = 'HonorSystem'
const quizKey = 'Quiz'
const videoKey = 'Video'

// const enabled = ref(props.initialSkillData.selfReportingEnabled)
const categories = ref([
  { name: 'Approval Queue', key: approvalKey },
  { name: 'Honor System', key: honorKey },
  { name: 'Quiz/Survey', key: quizKey },
  { name: 'Video', key: videoKey }
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
  if (newType === quizKey || newType === videoKey) {
    setFieldValue('numPerformToCompletion', 1)
    setFieldValue('timeWindowEnabled', false)
    setFieldValue('numPointIncrementMaxOccurrences', 1)
  }
  emit('selfReportingTypeChanged', newType)
}
const quizIdSelected = (quizId) => {
  emit('quizIdChanged', quizId)
}

const justificationRequired = useFieldValue('justificationRequired')
const selfReportingType = useFieldValue('selfReportingType')
const enabled = useFieldValue('selfReportingEnabled')
const quizSelected = computed(() => selfReportingType.value === quizKey)
const isVideoChoiceDisabled = computed(() => !enabled.value || !props.isEdit || (props.isEdit && !props.initialSkillData.hasVideoConfigured))
const isVideoConfigured = computed(() => props.initialSkillData.hasVideoConfigured)

const originalSelfReportingType = ref(selfReportingType.value)

const approvals = ref( {
  showWarning: false,
  loading: false,
  numPending: 0,
  numRejected: 0,
  newSelfReportingType: null,
})
const handleSelfReportingWarning =() => {
  if (props.isEdit) {
    approvals.value.showWarning = false;
    if (originalSelfReportingType.value === 'Approval' &&
        (selfReportingType.value === 'HonorSystem' || !enabled.value || selfReportingType.value === 'Video' || selfReportingType.value === 'Quiz')) {
      approvals.value.loading = true;
      SelfReportService.getSkillApprovalsStats(route.params.projectId, props.initialSkillData.skillId)
        .then((res) => {
          const pendingApprovalsRes = res.find((item) => item.value === 'SkillApprovalsRequests');
          if (pendingApprovalsRes) {
            approvals.value.numPending = pendingApprovalsRes.count;
          }

          const pendingRejectionsRes = res.find((item) => item.value === 'SkillApprovalsRejected');
          if (pendingRejectionsRes) {
            approvals.value.numRejected = pendingRejectionsRes.count;
          }

          approvals.value.showWarning = pendingApprovalsRes.count > 0 || pendingRejectionsRes.count > 0;
        })
        .finally(() => {
          approvals.value.loading = false;
        });
      approvals.value.newSelfReportingType = !enabled.value ? 'Disabled' : selfReportingType.value;
    } else {
      approvals.value.showWarning = false;
    }
  }
}

watch(() => selfReportingType.value, () => {
  handleSelfReportingWarning();
})
</script>

<template>
  <div>
    <div class="flex flex-wrap md:flex-no-wrap">
      <!--        @input="onEnabledChanged"-->
      <div class="w-min-10rem mt-4 w-full md:w-auto">
        <SkillsCheckboxInput
          @update:modelValue="onEnabledChanged"
          :binary="true"
          inputId="selfReport"
          name="selfReportingEnabled"
          data-cy=selfReportEnableCheckbox />
        <label for="selfReport" class="ml-2"> Self Reporting </label>
      </div>

      <div class="card flex flex-1 sm:ml-4 mt-4">
        <div class="flex flex-column gap-3 w-full" data-cy="selfReportTypeSelector">
          <div v-for="category in categories" :key="category.key" class="flex align-items-start">
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
                  class="ml-2 font-italic">Justification Required</label>
              </div>
              <div v-if="category.key === quizKey" class="pl-2 w-full">
                <QuizSelector class="mb-0" v-if="category.key === quizKey && enabled && quizSelected"
                              :initiallySelectedQuizId="initialSkillData.quizId" @changed="quizIdSelected" />
              </div>
              <span v-if="category.key === videoKey && !isVideoConfigured" class="ml-2 font-italic"
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
    <Message class="pt-1"
             v-if="approvals.showWarning"
             :closable="false"
             severity="warn"
             data-cy="selfReportingTypeWarning">
      <div v-if="approvals.newSelfReportingType === 'HonorSystem'">
        Switching this skill to the <i>Honor System</i> will automatically:
        <ul>
          <li v-if="approvals.numPending > 0">
            Approve <b>{{ numFormat.pretty(approvals.numPending) }} pending</b> request<span v-if="approvals.numPending>1">s</span>
          </li>
          <li v-if="approvals.numRejected > 0">
            Remove <b>{{ approvals.numRejected}} rejected</b> request<span v-if="approvals.numRejected>1">s</span>
          </li>
        </ul>
      </div>
      <div v-if="approvals.newSelfReportingType === 'Disabled' || approvals.newSelfReportingType === 'Quiz' || approvals.newSelfReportingType === 'Video'">
        Disabling <i>Self Reporting</i> will automatically:
        <ul>
          <li v-if="approvals.numPending > 0">
            Remove <b>{{ numFormat.pretty(approvals.numPending) }} pending</b> request<span v-if="approvals.numPending>1">s</span>
          </li>
          <li v-if="approvals.numRejected > 0">
            Remove <b>{{ approvals.numRejected}} rejected</b> request<span v-if="approvals.numRejected>1">s</span>
          </li>
        </ul>
      </div>
    </Message>
  </div>
</template>

<style scoped>

</style>