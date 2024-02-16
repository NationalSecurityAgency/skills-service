<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import { object, string, number } from 'yup'
import { useAppConfig } from '@/components/utils/UseAppConfig.js'
import SkillsNameAndIdInput from '@/components/utils/inputForm/SkillsNameAndIdInput.vue'
import SkillsService from '@/components/skills/SkillsService.js'
import TotalPointsField from '@/components/skills/inputForm/TotalPointsField.vue'
import TimeWindowInput from '@/components/skills/inputForm/TimeWindowInput.vue'
import SelfReportingTypeInput from '@/components/skills/inputForm/SelfReportingTypeInput.vue'
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import HelpUrlInput from '@/components/utils/HelpUrlInput.vue'
import InputSanitizer from '@/components/utils/InputSanitizer.js'
import { useDebounceFn } from '@vueuse/core'

const show = defineModel()
const route = useRoute()
const props = defineProps({
  skill: Object,
  isEdit: Boolean,
  isCopy: Boolean
})
const emit = defineEmits(['skill-saved'])
const appConfig = useAppConfig()

const latestSkillVersion = ref(0)
const maxSkillVersion = ref(1)

const asyncLoadData = () => {
  const loadSkillDetails = () => {
    if (props.isEdit) {
      return SkillsService.getSkillDetails(route.params.projectId, route.params.subjectId, props.skill.skillId)
        .then((resSkill) => {
          return { ...resSkill, 'description': resSkill.description || '' }
        })
    }

    return Promise.resolve({})
  }

  return loadSkillDetails().then((skillRes) => {
    return SkillsService.getLatestSkillVersion(route.params.projectId)
      .then((latestVersion) => {
        latestSkillVersion.value = latestVersion
        maxSkillVersion.value = Math.min(latestVersion + 1, appConfig.maxSkillVersion)
        return {
          ...skillRes,
          skipTheseAttrsWhenValidatingOnInit: ['version'],
          version: latestVersion
        }
      })
  })
}

const formId = props.isEdit ? `editSkillDialog-${props.skill.projectId}-${props.skill.skillId}` : 'newSkillDialog'
let modalTitle = 'New Skill'
if (props.isEdit) {
  modalTitle = 'Edit Skill'
}
if (props.isCopy) {
  modalTitle = 'Copy Skill'
}

const checkSkillNameUnique = useDebounceFn((value) => {
  if (!value || value.length === 0) {
    return true
  }
  const origName = props.skill.name
  if (props.isEdit && (origName === value || origName.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
    return true
  }
  return SkillsService.skillWithNameExists(route.params.projectId, value).then((remoteRes) => remoteRes)
}, appConfig.formFieldDebounceInMs)
const checkSkillIdUnique = useDebounceFn((value) => {
  if (!value || value.length === 0 || (props.isEdit && props.skill.skillId === value)) {
    return true
  }
  return SkillsService.skillWithIdExists(route.params.projectId, value)
    .then((remoteRes) => remoteRes)

}, appConfig.formFieldDebounceInMs)
const schema = object({
  'skillName': string()
    .trim()
    .required()
    .min(appConfig.minNameLength)
    .max(appConfig.maxSkillNameLength)
    .nullValueNotAllowed()
    .customNameValidator('Skill Name')
    .test('uniqueName', 'The value for the Skill Name is already taken', (value) => checkSkillNameUnique(value))
    .label('Skill Name'),
  'skillId': string()
    .required()
    .min(appConfig.minIdLength)
    .max(appConfig.maxIdLength)
    .nullValueNotAllowed()
    .matches(/^[\w%]+$/, (fieldProps) => `${fieldProps.label} may only contain alpha-numeric, underscore or percent characters`)
    .test('uniqueId', 'The value for the Skill ID is already taken', (value) => checkSkillIdUnique(value))
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
      async (value) => (latestSkillVersion.value + 1) >= value
    )
    .label('Version'),
  'helpUrl': string()
    .urlValidator()
    .nullable()
    .label('Help URL'),
})
const selfReportingType = props.skill.selfReportingType && props.skill.selfReportingType !== 'Disabled' ? props.skill.selfReportingType : null
const initialSkillData = {
  skillId: props.skill.skillId || '',
  skillName: props.skill.name || '',
  originalSkillId: props.skill.skillId || '',
  version: props.skill.verison || 0,
  pointIncrement: props.skill.pointIncrement || 100,
  numPerformToCompletion: props.skill.numPerformToCompletion || 1,
  pointIncrementIntervalHrs: props.skill.pointIncrementIntervalHrs || 8,
  pointIncrementIntervalMins: props.skill.pointIncrementIntervalMins || 0,
  numMaxOccurrencesIncrementInterval: props.skill.numMaxOccurrencesIncrementInterval || 1,
  numPointIncrementMaxOccurrences: props.skill.numPointIncrementMaxOccurrences || 1,
  selfReportingType,
  selfReportingEnabled: selfReportingType !== null,
  description: props.skill.description || ''
}

const saveSkill = (values) => {
  const skilltoSave = {
    ...values,
    type: 'Skill',
    subjectId: route.params.subjectId,
    projectId: route.params.projectId,
    isEdit: props.isEdit,
    name: InputSanitizer.sanitize(values.skillName),
    skillId: InputSanitizer.sanitize(values.skillId),
    pointIncrementInterval: values.timeWindowEnabled ? values.pointIncrementIntervalHrs * 60 + values.pointIncrementIntervalMins : 0,
    selfReportingType: values.selfReportingType && values.selfReportingType !== 'Disabled' ? values.selfReportingType : null,
  }
  return SkillsService.saveSkill(skilltoSave)
    .then((skillRes) => {
      return {
        ...skillRes,
        originalSkillId: props.skill.skillId,
      }
    })
  // close()
}

const onSkillSaved = (skill) => {
  emit('skill-saved', skill)
}

const close = () => {

}
</script>

<template>
  <SkillsInputFormDialog
    :id="formId"
    v-model="show"
    :async-load-data-function="asyncLoadData"
    :save-data-function="saveSkill"
    :header="modalTitle"
    saveButtonLabel="Save"
    :validation-schema="schema"
    :initial-values="initialSkillData"
    :enable-return-focus="true"
    @saved="onSkillSaved"
    @close="close">
    <div class="flex flex-wrap">
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

      <div class="lg:max-w-10rem lg:ml-3 w-full">
        <SkillsNumberInput
          showButtons
          :min="latestSkillVersion"
          data-cy="skillVersion"
          label="Version"
          name="version" />
      </div>
    </div>

    <div class="flex flex-wrap lg:flex-no-wrap">
      <SkillsNumberInput
        class="flex-1"
        style="min-width: 14rem;"
        :min="1"
        :is-required="true"
        label="Point Increment"
        name="pointIncrement" />

      <SkillsNumberInput
        class="flex-1 sm:ml-2"
        style="min-width: 16rem;"
        showButtons
        :min="0"
        :is-required="true"
        label="Occurrences to Completion"
        name="numPerformToCompletion" />

      <total-points-field class="lg:ml-2" />
    </div>

    <time-window-input
      :time-window-enabled-default="skill.timeWindowEnabled"
      class="mb-3"/>

    <self-reporting-type-input class="mt-1"/>

    <markdown-editor
      class="mt-5"
      name="description" />

    <help-url-input class="mt-3"
                    name="helpUrl" />

  </SkillsInputFormDialog>
</template>

<style scoped>

</style>