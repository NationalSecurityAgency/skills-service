/*
Copyright 2026 SkillTree

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
import { boolean, number, object, string } from 'yup'
import {useFieldValue, useForm} from 'vee-validate';
import SkillsDialog from "@/components/utils/inputForm/SkillsDialog.vue";
import {useAppConfig} from '@/common-components/stores/UseAppConfig';
import {useSkillsAnnouncer} from '@/common-components/utilities/UseSkillsAnnouncer';
import {usePluralize} from "@/components/utils/misc/UsePluralize.js";
import {useFocusState} from "@/stores/UseFocusState.js";
import {computed, ref} from "vue";
import TotalPointsField from "@/components/skills/inputForm/TotalPointsField.vue";
import SkillsService from "@/components/skills/SkillsService.js";
import {useRoute} from "vue-router";
import LengthyOperationProgressBar from "@/components/utils/LengthyOperationProgressBar.vue";
import SkillsDropDown from "@/components/utils/inputForm/SkillsDropDown.vue";
import SkillsCheckboxInput from "@/components/utils/inputForm/SkillsCheckboxInput.vue";
import TimeWindowInput from "@/components/skills/inputForm/TimeWindowInput.vue";
import PendingApprovalsWarning from "@/components/skills/inputForm/PendingApprovalsWarning.vue";
import SelfReportType from "@/components/skills/selfReport/SelfReportType.js";

const props = defineProps({
  skills: Array,
});

const appConfig = useAppConfig();
const route = useRoute()
const announcer = useSkillsAnnouncer();
const pluralize = usePluralize();
const model = defineModel()
const focusState = useFocusState()
const emit = defineEmits(['skills-updated'])

const validateMoreThanWindowMaxOccurrences = (value, testContext) => {
  const parent = testContext.parent
  const nullChecks = parent.numPerformToCompletion === null || parent.numPointIncrementMaxOccurrences === null
  return nullChecks || parent.numPointIncrementMaxOccurrences <= value
}
const validateLessThanTotalOccurrences = (value, testContext) => {
  const parent = testContext.parent
  const nullChecks = parent.numPointIncrementMaxOccurrences === null || parent.numPerformToCompletion === null
  return nullChecks || parent.numPerformToCompletion >= value
}
const validateMustHaveHoursIfMins = (value, testContext) => {
  const parent = testContext.parent
  const nullChecks = parent.pointIncrementIntervalHrs === null || parent.pointIncrementIntervalMins === null
  return nullChecks || (parent.pointIncrementIntervalMins > 0 || value > 0)
}

const validateMustHaveMinsIfHours = (value, testContext) => {
  const parent = testContext.parent
  const nullChecks = parent.pointIncrementIntervalMins === null || parent.pointIncrementIntervalHrs === null
  return nullChecks || (parent.pointIncrementIntervalHrs > 0 || value > 0)
}
const schema = object({
  pointIncrement: number().min(1).max(appConfig.maxPointIncrement).nullable().label('Point Increment'),
  numPerformToCompletion: number()
      .min(1)
      .max(appConfig.maxPointIncrement)
      .test(
          'moreThanWindowOccurrences',
          ({ label }) => `${label} must be >= Window's Max Occurrences`,
          async (value, testContext) => validateMoreThanWindowMaxOccurrences(value, testContext)
      )
      .label('Occurrences').nullable(),
  'numPointIncrementMaxOccurrences': number()
      .min(1)
      .max(appConfig.maxNumPointIncrementMaxOccurrences)
      .test(
          'lessThanTotalOccurrences',
          ({ label }) => `${label} must be <= total Occurrences to Completion`,
          async (value, testContext) => validateLessThanTotalOccurrences(value, testContext)
      )
      .label('Max Occurrences').nullable(),
  'pointIncrementIntervalHrs': number()
      .required()
      .min(0)
      .max(appConfig.maxTimeWindowInHrs)
      .test(
          'mustHaveHoursIfMinsAre0',
          'Hours must be > 0 if Minutes = 0',
          async (value, testContext) => validateMustHaveHoursIfMins(value, testContext)
      )
      .label('Hours').nullable(),
  'pointIncrementIntervalMins': number()
      .required()
      .min(0)
      .max(60)
      .test(
          'mustHaveMinsIfHoursAre0',
          'Minutes must be > 0 if Hours = 0',
          async (value, testContext) => validateMustHaveMinsIfHours(value, testContext)
      )
      .label('Minutes').nullable(),
});

const initialValues = {
  pointIncrement: null,
  numPerformToCompletion: null,
  numPointIncrementMaxOccurrences: null,
  pointIncrementIntervalHrs: null,
  pointIncrementIntervalMins: null,
  selfReportingType: null,
  enabled: null,
}
const { values, meta, validate } = useForm({ validationSchema: schema, initialValues });
const selfReportingType = useFieldValue('selfReportingType')

const isUpdating = ref(false)
const approvalKey = 'Approval'
const honorKey = 'HonorSystem'
const selfReportOptions = [
  { name: 'Approval Queue', key: approvalKey },
  { name: 'Honor System', key: honorKey },
]
const skillsIds = props.skills.map((it) => it.skillId)

const close = () => {
  model.value = false
  focusState.focusOnLastElement()
}

const occurrencesToCompletionAndTimeWindowDisabled = computed(() => {
  return (props.skills.indexOf((it) => it.selfReportingType.value === 'Quiz') === undefined)
})

const validateThenUpdate = (values) => {
  validate().then(({valid}) => {
    if (valid) {
      doUpdate()
    }
  })
}
const doUpdate = () => {
  isUpdating.value = true
  const updateInfo = {
    ...values,
    skills: skillsIds,
    selfReportingType: values.selfReportingType?.key,
    numMaxOccurrencesIncrementInterval: values.numPointIncrementMaxOccurrences
  }
  if (values.pointIncrementIntervalHrs !== null && values.pointIncrementIntervalMins !== null ) {
    updateInfo.pointIncrementInterval = values.pointIncrementIntervalHrs * 60 + values.pointIncrementIntervalMins;
    if (values.numPointIncrementMaxOccurrences !== null) {
      updateInfo.numMaxOccurrencesIncrementInterval = values.numPointIncrementMaxOccurrences;
    }
  }

  SkillsService.batchSkillsUpdate(route.params.projectId, updateInfo)
      .finally(() => {
        isUpdating.value = false
        emit('skills-updated')
        announcer.polite(`Updated ${props.skills.length} ${pluralize.plural('Skill', props.skills.length)}`)
        close()
      })
}

const numOfDisabledSkills = computed(() => props.skills.filter((it) => it.enabled === false || it.enabled === "false").length)
const hasDisabledSkills = computed(() => numOfDisabledSkills.value > 0)
</script>

<template>
  <SkillsDialog
      v-model="model"
      :header="`Batch Edit ${skills.length} ${pluralize.plural('Skill', skills.length)}`"
      :enable-return-focus="true"
      @on-cancel="close"
      ok-button-label="Update"
      :ok-button-disabled="!meta.valid"
      @on-ok="validateThenUpdate"
  >
    <div class="pb-5 flex flex-col gap-3">
      <Message :closable="false" data-cy="batchUpdateMsg">Update
        <Tag>{{ skills.length }}</Tag>
        {{ pluralize.plural('Skill', skills.length) }} at once. Provide at least <b>one</b> attribute below to apply changes.
      </Message>
      <lengthy-operation-progress-bar v-if="isUpdating "/>
      <div v-if="!isUpdating"
          class="flex flex-col md:flex-row gap-2 mt-2">

        <SkillsNumberInput
            class="flex-1 min-w-[13rem]"
            :min="1"
            :is-required="true"
            label="Point Increment"
            name="pointIncrement" />

        <SkillsNumberInput
            class="flex-1 min-w-[15rem]"
            :min="0"
            :is-required="true"
            :disabled="occurrencesToCompletionAndTimeWindowDisabled"
            label="Occurrences to Completion"
            name="numPerformToCompletion" />

        <total-points-field class="min-w-[8rem]"/>
      </div>

      <time-window-input class="mb-4">
        <template #message>
          <Message :closable="false" severity="warn" icon="fa-solid fa-triangle-exclamation">Applies only to skills with <span class="italic font-bold">Occurrences to Completion</span> greater than 1 </Message>
        </template>
      </time-window-input>

      <SkillsDropDown
          label="Self Report Type"
          optionLabel="name"
          :options="selfReportOptions"
          :show-clear-button="true"
          name="selfReportingType" />

      <pending-approvals-warning
          :self-reporting-type="selfReportingType?.key || SelfReportType.Approval"
          :self-report-enabled="true"
          :skill-ids="skillsIds"/>

      <div v-if="hasDisabledSkills" class="flex gap-2 items-center mt-1">
        <label for="enabledCheckbox">Visibility:</label>
        <div class="flex gap-2 items-center">
          <SkillsCheckboxInput
              :binary="true"
              name="enabled"/>
          <div>Visible</div>
        </div>

        <div class="italic" data-cy="hiddenSkillsMsg">( <b>{{ numOfDisabledSkills }}</b> {{ pluralize.plural('Skill', numOfDisabledSkills) }}
          currently hidden )
        </div>
      </div>

    </div>

  </SkillsDialog>
</template>

<style scoped>
</style>