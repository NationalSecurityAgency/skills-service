<script setup>
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import { object, string } from 'yup'
import { useAppConfig } from '@/components/utils/UseAppConfig.js'
import SkillsNameAndIdInput from '@/components/utils/inputForm/SkillsNameAndIdInput.vue'

const show = defineModel()
const props = defineProps({
  skill: Object,
  isEdit: Boolean,
  isCopy: Boolean
})
const appConfig = useAppConfig()

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
const initialSkillData = {
  originalSkillId: props.skill.skillId,
  ...props.skill,
  skillName: props.skill.name,
  description: props.skill.description || ''
}

const updateSkills = () => {

}
const close = () => {

}
</script>

<template>
  <SkillsInputFormDialog
    :id="formId"
    v-model="show"
    :header="modalTitle"
    saveButtonLabel="Save"
    :validation-schema="schema"
    :initial-values="initialSkillData"
    :enable-return-focus="true"
    @saved="updateSkills"
    @close="close">
    <div class="flex">
      <div class="flex-1">
        <SkillsNameAndIdInput
          :name-label="`${isCopy ? 'New Skill Name' : 'Skill Name'}`"
          name-field-name="skillName"
          :id-label="`${props.isCopy ? 'New Skill ID' : 'Skill ID'}`"
          id-field-name="skillId"
          :is-inline="true"
          :name-to-id-sync-enabled="!props.isEdit" />
      </div>

      <div class="">
        <SkillsNumberInput
          class="ml-3"
          label="Version"
          name="version" />
      </div>
    </div>

  </SkillsInputFormDialog>
</template>

<style scoped>

</style>