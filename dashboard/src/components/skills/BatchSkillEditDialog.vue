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
import * as yup from 'yup';
import { useForm } from 'vee-validate';
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

const schema = yup.object({
  pointIncrement: yup.number().min(1).max(appConfig.maxPointIncrement).nullable().label('Point Increment'),
  numPerformToCompletion: yup.number()
      .min(1)
      .max(appConfig.maxPointIncrement)
      .test(
          'moreThanWindowOccurrences',
          ({ label }) => `${label} must be >= Window's Max Occurrences`,
          async (value, testContext) => testContext.parent.numPointIncrementMaxOccurrences === null || testContext.parent.numPointIncrementMaxOccurrences <= value
      )
      .label('Occurrences').nullable(),
});

const initialValues = {
  pointIncrement: null,
  numPerformToCompletion: null,
  numPointIncrementMaxOccurrences: null,
  selfReportingType: null,
  enabled: null,
}
const { values, errors, defineField, resetForm, validate } = useForm({ validationSchema: schema, initialValues });

const isUpdating = ref(false)
const approvalKey = 'Approval'
const honorKey = 'HonorSystem'
const selfReportOptions = [
  { name: 'Approval Queue', key: approvalKey },
  { name: 'Honor System', key: honorKey },
]

const close = () => {
  model.value = false
  focusState.focusOnLastElement()
}

// todo: need to add a service tests for this too
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
    skills: props.skills.map((it) => it.skillId),
    selfReportingType: values.selfReportingType?.key,
  }
  SkillsService.batchSkillsUpdate(route.params.projectId, updateInfo)
      .finally(() => {
        isUpdating.value = false
        emit('skills-updated')
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
      @on-ok="validateThenUpdate"
  >
    <div class="pb-5 flex flex-col gap-3">
      <Message :closable="false">Update
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

      <SkillsDropDown
          label="Self Report Type"
          optionLabel="name"
          :options="selfReportOptions"
          name="selfReportingType" />

      <div v-if="hasDisabledSkills" class="flex gap-2 items-center mt-1">
        <label>Visibility:</label>
        <div class="flex gap-2 items-center">
          <SkillsCheckboxInput
              :binary="true"
              name="enabled"/>
          <div>Visible</div>
        </div>

        <div class="italic">( <b>{{ numOfDisabledSkills }}</b> {{ pluralize.plural('Skill', numOfDisabledSkills) }}
          currently hidden )
        </div>
      </div>

    </div>

  </SkillsDialog>
</template>

<style scoped>
</style>