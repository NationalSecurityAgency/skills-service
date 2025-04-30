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
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import { boolean, number, object, string } from 'yup'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import SkillsNameAndIdInput from '@/components/utils/inputForm/SkillsNameAndIdInput.vue'
import SkillsService from '@/components/skills/SkillsService.js'
import TotalPointsField from '@/components/skills/inputForm/TotalPointsField.vue'
import TimeWindowInput from '@/components/skills/inputForm/TimeWindowInput.vue'
import SelfReportingTypeInput from '@/components/skills/inputForm/SelfReportingTypeInput.vue'
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import HelpUrlInput from '@/components/utils/HelpUrlInput.vue'
import InputSanitizer from '@/components/utils/InputSanitizer.js'
import { useSkillYupValidators } from '@/components/skills/UseSkillYupValidators.js'
import SettingsService from '@/components/settings/SettingsService.js'
import { useCommunityLabels } from '@/components/utils/UseCommunityLabels.js'
import SkillsInputSwitch from '@/components/utils/inputForm/SkillsInputSwitch.vue'
import InputGroup from 'primevue/inputgroup'
import InputGroupAddon from 'primevue/inputgroupaddon'

const show = defineModel()
const route = useRoute()
const props = defineProps({
  skill: Object,
  isEdit: Boolean,
  isCopy: Boolean,
  groupId: {
    type: String,
    default: null,
  },
  projectUserCommunity: String,
})
const emit = defineEmits(['skill-saved'])
const appConfig = useAppConfig()
const communityLabels = useCommunityLabels()
const skillYupValidators = useSkillYupValidators()

const latestSkillVersion = ref(0)
const maxSkillVersion = ref(1)

const asyncLoadData = () => {
  const loadSkillDetails = () => {
    if (props.isEdit || props.isCopy) {
      return SkillsService.getSkillDetails(route.params.projectId, route.params.subjectId, props.skill.skillId)
        .then((resSkill) => {
          const skillDetails = {
            ...resSkill,
            'description': resSkill.description || '',
            skillName: props.isCopy ? `Copy of ${resSkill.name}` : resSkill.name,
            skillId: props.isCopy ? `copy_of_${resSkill.skillId}` : resSkill.skillId,
            selfReportingType: props.isCopy && resSkill.selfReportingType === 'Video' ? 'Approval' : resSkill.selfReportingType,
          }
          initialSkillData.value = { ...skillDetails };
          initialSkillData.value.hasVideoConfigured = resSkill.hasVideoConfigured;
          skillEnabled.value = resSkill.enabled
          return skillDetails;
        })
    }
    return Promise.resolve({})
  }

  const findLatestSkillVersion = (skillRes) => {
    if (!props.isEdit) {
      return SkillsService.getLatestSkillVersion(route.params.projectId).then((latestVersion) => {
        latestSkillVersion.value = latestVersion
        maxSkillVersion.value = Math.min(latestVersion + 1, appConfig.maxSkillVersion)
        return {
          ...skillRes,
          skipTheseAttrsWhenValidatingOnInit: ['version'],
          version: latestVersion
        }
      })
    }
    return Promise.resolve(skillRes)
  }

  const getProjectDefaults = (skillResWithVersion) => {
    if (!props.isEdit && !props.copy) {
      return SettingsService.getSettingsForProject(route.params.projectId).then((settings) => {
        if (settings) {
          const selfReportingTypeSetting = settings.find((item) => item.setting === 'selfReport.type');
          if (selfReportingTypeSetting) {
            skillResWithVersion.selfReportingType = selfReportingTypeSetting.value;
            skillResWithVersion.skipTheseAttrsWhenValidatingOnInit.push('selfReportingType');
            if (selfReportingTypeSetting.value !== 'Disabled') {
              skillResWithVersion.selfReportingEnabled = true;
              skillResWithVersion.skipTheseAttrsWhenValidatingOnInit.push('selfReportingEnabled');
            }
          }
          const selfReportingJustificationSetting = settings.find((item) => item.setting === 'selfReport.justificationRequired');
          if (selfReportingJustificationSetting) {
            skillResWithVersion.justificationRequired = selfReportingJustificationSetting.value && selfReportingJustificationSetting.value !== 'false';
            skillResWithVersion.skipTheseAttrsWhenValidatingOnInit.push('justificationRequired');
          }
        }
        initialSkillData.value = { ...skillResWithVersion };
        return skillResWithVersion;
      })
    }
    return Promise.resolve(skillResWithVersion)
  }

  return loadSkillDetails().then((skillRes) => {
    return findLatestSkillVersion(skillRes).then((skillResWithVersion) => {
      return getProjectDefaults(skillResWithVersion)
    })
  })
}

let formId = 'newSkillDialog'
let modalTitle = 'New Skill'
if (props.isEdit) {
  modalTitle = 'Edit Skill'
  formId = `editSkillDialog-${props.skill.projectId}-${props.skill.skillId}`
}
if (props.isCopy) {
  modalTitle = 'Copy Skill'
  formId = `copySkillDialog-${props.skill.projectId}-${props.skill.skillId}`
}
const isQuizAssignableToSkill = (selectedQuiz, context) => {
  const quizIsRestricted = communityLabels.isRestrictedUserCommunity(selectedQuiz?.userCommunity);
  const projectIsRestricted = communityLabels.isRestrictedUserCommunity(props.projectUserCommunity);
  if (selectedQuiz && quizIsRestricted && !projectIsRestricted) {
    return context.createError({message: `Quiz is not allowed to be assigned to this project. Project does not have ${selectedQuiz.userCommunity} permission`});
  }
  return true;
}

const schema = object({
  'skillName': string()
    .trim()
    .required()
    .min(appConfig.minNameLength)
    .max(appConfig.maxSkillNameLength)
    .nullValueNotAllowed()
    .customNameValidator('Skill Name')
    .test('uniqueName', 'The value for the Skill Name is already taken', (value) => skillYupValidators.checkSkillNameUnique(value, props.skill.name, props.isEdit))
    .label('Skill Name'),
  'skillId': string()
    .required()
    .min(appConfig.minIdLength)
    .max(appConfig.maxIdLength)
    .nullValueNotAllowed()
    .matches(/^[\w%]+$/, (fieldProps) => `${fieldProps.label} may only contain alpha-numeric, underscore or percent characters`)
    .test('uniqueId', 'The value for the Skill ID is already taken', (value) => skillYupValidators.checkSkillIdUnique(value, props.skill.skillId, props.isEdit))
    .label('Skill ID'),
  'description': string()
    .max(appConfig.descriptionMaxLength)
    .customDescriptionValidator('Skill Description')
    .label('Skill Description'),
  'pointIncrement': number()
    .required()
    .min(1)
    .max(appConfig.maxPointIncrement)
    .label('Point Increment'),
  'numPerformToCompletion': number()
    .required()
    .min(1)
    .max(appConfig.maxPointIncrement)
    .test(
      'moreThanWindowOccurrences',
      ({ label }) => `${label} must be >= Window's Max Occurrences`,
      async (value, testContext) => testContext.parent.numPointIncrementMaxOccurrences <= value
    )
    .label('Occurrences'),
  'pointIncrementIntervalHrs': number()
    .required()
    .min(0)
    .max(appConfig.maxTimeWindowInHrs)
    .test(
      'mustHaveHoursIfMinsAre0',
      'Hours must be > 0 if Minutes = 0',
      async (value, testContext) => testContext.parent.pointIncrementIntervalMins > 0 || value > 0
    )
    .label('Hours'),
  'pointIncrementIntervalMins': number()
    .required()
    .min(0)
    .max(60)
    .test(
      'mustHaveMinsIfHoursAre0',
      'Minutes must be > 0 if Hours = 0',
      async (value, testContext) => testContext.parent.pointIncrementIntervalHrs > 0 || value > 0
    )
    .label('Minutes'),
  'numPointIncrementMaxOccurrences': number()
    .required()
    .min(1)
    .max(appConfig.maxNumPointIncrementMaxOccurrences)
    .test(
      'lessThanTotalOccurrences',
      ({ label }) => `${label} must be <= total Occurrences to Completion`,
      async (value, testContext) => testContext.parent.numPerformToCompletion >= value
    )
    .label('Max Occurrences'),
  'version': number()
    .required()
    .min(0)
    .max(appConfig.maxSkillVersion)
    .test(
      'maxNextVersion',
      ({ label }) => `${label} ${latestSkillVersion.value} is the latest; max supported version is 1 (latest + 1)`,
      async (value) => props.isEdit || (latestSkillVersion.value + 1) >= value
    )
    .label('Version'),
  'enabled': boolean(),
  'helpUrl': string()
    .urlValidator()
    .nullable()
    .label('Help URL'),
  'associatedQuiz': object()
      .nullable()
      .test('quizRequired', 'Please select an available Quiz/Survey', (value) => !!(selfReportingType.value !== 'Quiz' || value))
      .test('quizIsAssignable', (value, context) => isQuizAssignableToSkill(value, context))
      .label('Quiz/Survey'),
})
const selfReportingType = ref(props.skill.selfReportingType && props.skill.selfReportingType !== 'Disabled' ? props.skill.selfReportingType : null)
const initialSkillData = ref({
  skillId: props.skill.skillId || '',
  skillName: props.skill.name || '',
  originalSkillId: props.skill.skillId || '',
  version: props.skill.verison || 0,
  enabled: props.skill.enabled || true,
  pointIncrement: props.skill.pointIncrement || 100,
  numPerformToCompletion: props.skill.numPerformToCompletion || 1,
  timeWindowEnabled: props.skill.timeWindowEnabled || false,
  pointIncrementIntervalHrs: props.skill.pointIncrementIntervalHrs || 8,
  pointIncrementIntervalMins: props.skill.pointIncrementIntervalMins || 0,
  numMaxOccurrencesIncrementInterval: props.skill.numMaxOccurrencesIncrementInterval || 1,
  numPointIncrementMaxOccurrences: props.skill.numPointIncrementMaxOccurrences || 1,
  selfReportingType: selfReportingType.value ? selfReportingType.value : 'Disabled',
  selfReportingEnabled: selfReportingType.value !== null,
  description: props.skill.description || '',
  quizId: props.skill.quizId
})

const saveSkill = (values) => {
  const skilltoSave = {
    ...values,
    type: 'Skill',
    subjectId: route.params.subjectId,
    projectId: route.params.projectId,
    isEdit: props.isEdit,
    groupId: props.groupId,
    name: InputSanitizer.sanitize(values.skillName),
    skillId: InputSanitizer.sanitize(values.skillId),
    quizId: values.associatedQuiz ? values.associatedQuiz.quizId : null,
    pointIncrementInterval: values.timeWindowEnabled ? values.pointIncrementIntervalHrs * 60 + values.pointIncrementIntervalMins : 0,
    selfReportingType: values.selfReportingType && values.selfReportingType !== 'Disabled' ? values.selfReportingType : null,
  }
  return SkillsService.saveSkill(skilltoSave)
    .then((skillRes) => {
      return {
        ...skillRes,
        originalSkillId: !props.isCopy ? props.skill.skillId : null,
      }
    })
  // close()
}

const onSkillSaved = (skill) => {
  emit('skill-saved', skill)
}

const occurrencesToCompletionAndTimeWindowDisabled = computed(() => {
  return (selfReportingType.value === 'Quiz' || selfReportingType.value === 'Video')
})
const skillEnabled = ref(true)
const onEnabledChanged = (event) => {
  skillEnabled.value = !skillEnabled.value
}
const showVisibilityControl = computed(() => {
  // always show on create, only show on edit if currently disabled
  return !(props.isEdit && props.skill.enabled);
})

</script>

<template>
  <SkillsInputFormDialog
    :id="formId"
    v-model="show"
    :should-confirm-cancel="true"
    :is-edit="isEdit"
    :async-load-data-function="asyncLoadData"
    :save-data-function="saveSkill"
    :header="modalTitle"
    saveButtonLabel="Save"
    :validation-schema="schema"
    :initial-values="initialSkillData"
    :enable-return-focus="true"
    @saved="onSkillSaved"
  >
    <div v-if="showVisibilityControl">
      <div class="flex flex-wrap flex-col md:flex-row gap-2">
        <div class="flex-1">
          <SkillsNameAndIdInput
            :name-label="`${isCopy ? 'New Skill Name' : 'Skill Name'}`"
            name-field-name="skillName"
            :id-label="`${props.isCopy ? 'New Skill ID' : 'Skill ID'}`"
            id-field-name="skillId"
            :is-inline="true"
            id-suffix="Skill"
            :name-to-id-sync-enabled="!props.isEdit" />
        </div>
      </div>

      <div class="flex flex-col md:flex-row md:flex-1 gap-2 mt-2 pb-2">
              <div data-cy="visibility" class="flex-1 min-w-[8rem]">
                <div class="flex flex-col gap-2">
                  <label for="visibilitySwitch">
                    <span id="visibilityLabel">Initial Visibility:</span>
                  </label>
                  <InputGroup>
                    <InputGroupAddon>
                      <div style="width: 3.3rem !important;">
                        <SkillsInputSwitch data-cy="visibilitySwitch"
                                           aria-labelledby="visibilityLabel"
                                           inputId="visibilitySwitch"
                                           style="height:1rem !important;"
                                           size="small"
                                           name="enabled"
                                           @change="onEnabledChanged" />
                      </div>
                    </InputGroupAddon>
                    <InputGroupAddon class="w-full">
                      <span class="ml-2 w-full text-gray-700 dark:text-white">{{ skillEnabled ? 'Visible' : 'Hidden'}}</span>
                    </InputGroupAddon>
                  </InputGroup>
                </div>
              </div>

        <div class="flex-1">
          <SkillsNumberInput
              class="flex-1 min-w-[13rem] w-full"
              showButtons
              :disabled="isEdit"
              :min="latestSkillVersion"
              label="Version"
              name="version" />
        </div>
      </div>
    </div>
    <div v-else class="flex flex-wrap">
      <div class="flex-1">
        <SkillsNameAndIdInput
            :name-label="`${isCopy ? 'New Skill Name' : 'Skill Name'}`"
            name-field-name="skillName"
            :id-label="`${props.isCopy ? 'New Skill ID' : 'Skill ID'}`"
            id-field-name="skillId"
            :is-inline="true"
            id-suffix="Skill"
            :name-to-id-sync-enabled="!props.isEdit" />
      </div>

      <div class="lg:max-w-40 lg:ml-4 w-full">
        <SkillsNumberInput
            showButtons
            :disabled="isEdit"
            :min="latestSkillVersion"
            label="Version"
            name="version" />
      </div>
    </div>

    <div class="flex flex-col md:flex-row gap-2 mt-2">
      <SkillsNumberInput
        class="flex-1 min-w-[13rem]"
        :min="1"
        :is-required="true"
        label="Point Increment"
        name="pointIncrement" />

      <SkillsNumberInput
        class="flex-1 min-w-[15rem]"
        showButtons
        :min="0"
        :is-required="true"
        :disabled="occurrencesToCompletionAndTimeWindowDisabled"
        label="Occurrences to Completion"
        name="numPerformToCompletion" />

      <total-points-field class="min-w-[8rem]"/>
    </div>

    <time-window-input :disabled="occurrencesToCompletionAndTimeWindowDisabled" class="mb-4"/>

    <self-reporting-type-input @self-reporting-type-changed="selfReportingType = $event" :initial-skill-data="initialSkillData" :is-edit="isEdit" class="mt-1"/>

    <markdown-editor
      class="mt-8"
      name="description" />

    <help-url-input class="mt-4"
                    name="helpUrl" />

  </SkillsInputFormDialog>
</template>

<style scoped>

</style>