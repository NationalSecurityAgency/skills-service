<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import { object, string } from 'yup'
import { useAppConfig } from '@/components/utils/UseAppConfig.js'
import SkillsNameAndIdInput from '@/components/utils/inputForm/SkillsNameAndIdInput.vue'
import SkillsService from '@/components/skills/SkillsService.js'
import TotalPointsField from '@/components/skills/inputForm/TotalPointsField.vue'
import TimeWindowInput from '@/components/skills/inputForm/TimeWindowInput.vue'
import SelfReportingTypeInput from '@/components/skills/inputForm/SelfReportingTypeInput.vue'
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import HelpUrlInput from '@/components/utils/HelpUrlInput.vue'
import InputSanitizer from '@/components/utils/InputSanitizer.js'

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
  return SkillsService.getLatestSkillVersion(route.params.projectId)
    .then((latestVersion) => {
      latestSkillVersion.value = latestVersion
      maxSkillVersion.value = Math.min(latestVersion + 1, appConfig.maxSkillVersion)
      return {
        version: latestVersion
      }
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

const schema = object({
  'skillName': string()
    .trim()
    .required()
    .nullValueNotAllowed()
    .customNameValidator()
    .label('Skill Name'),
  'skillId': string()
    .required()
    .min(appConfig.minIdLength)
    .max(appConfig.maxIdLength)
    .nullValueNotAllowed()
    .label('Skill Id'),
  'description': string()
    .max(appConfig.descriptionMaxLength)
    .label('Skill Description'),
  'version': string()
    .trim()
    .required()
    .label('Version')
})
const selfReportingType = props.skill.selfReportingType && props.skill.selfReportingType !== 'Disabled' ? props.skill.selfReportingType : null
const initialSkillData = {
  skillId: props.skill.skillId || '',
  skillName: props.skill.name || '',
  originalSkillId: props.skill.skillId || '',
  version: props.skill.verison || 0,
  pointIncrement: props.skill.pointIncrement || 100,
  numPerformToCompletion: props.skill.numPerformToCompletion || 1,
  pointIncrementIntervalHrs: props.skill.pointIncrement || 8,
  pointIncrementIntervalMins: props.skill.pointIncrement || 0,
  numMaxOccurrencesIncrementInterval: props.skill.pointIncrement || 1,
  numPointIncrementMaxOccurrences: props.skill.pointIncrement || 1,
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
    pointIncrementInterval: values.timeWindowEnabled ? values.pointIncrementIntervalHrs * 60 + values.pointIncrementIntervalMins : 0
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
    <div class="flex">
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

      <SkillsNumberInput
        showButtons
        :min="latestSkillVersion"
        :max="maxSkillVersion"
        class="ml-3"
        label="Version"
        name="version" />
    </div>

    <div class="flex">
      <SkillsNumberInput
        class="flex-1"
        :min="0"
        :is-required="true"
        label="Point Increment"
        name="pointIncrement" />

      <SkillsNumberInput
        class="flex-1 ml-2"
        showButtons
        :min="0"
        :is-required="true"
        label="Occurrences to Completion"
        name="numPerformToCompletion" />

      <total-points-field class="ml-2" />
    </div>

    <time-window-input class="mb-3"/>

    <self-reporting-type-input class="mt-5"/>

    <markdown-editor
      class="mt-5"
      name="description" />

    <help-url-input class="mt-3"
                    name="helpUrl" />

  </SkillsInputFormDialog>
</template>

<style scoped>

</style>