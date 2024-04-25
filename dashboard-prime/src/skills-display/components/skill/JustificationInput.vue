<script setup>
import { ref, computed } from 'vue'
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { object, string } from 'yup'
import { useForm } from 'vee-validate'

const props = defineProps({
    isHonorSystem: Boolean,
    isApprovalRequired: Boolean,
    isJustitificationRequired: Boolean,
    skill: Object
  }
)
const emit = defineEmits(['cancel', 'report-skill'])

const attributes = useSkillsDisplayAttributesState()
const approvalRequestedMsg = ref('')


let justificationValiator = string()
  .max(attributes.maxSelfReportMessageLength)
  .customDescriptionValidator('Skill Description')
  .label('Justification')
if (props.isJustitificationRequired) {
  justificationValiator = justificationValiator.required()
}
const validationSchema = object({
  'justification': justificationValiator,
})
const { meta, handleSubmit, isSubmitting } = useForm({
  validationSchema: validationSchema,
  initialValues: { justification : ''}
})

const cancel = () => {
  emit('cancel');
}
const submit = handleSubmit(formValues => {
  emit('report-skill', formValues.justification);
})

</script>

<template>
  <Card>
    <template #content>
      <div
        :id="`reportSkillMsg-${skill.skillId}`"
        class="mb-2 skills-theme-primary-color"
        role="alert"
        data-cy="selfReportSkillMsg">
        ** Submit with {{ isJustitificationRequired ? 'a' : 'an' }}
       <b><span v-if="!isJustitificationRequired" class="text-muted">optional</span> justification</b> and it will enter an
        approval queue.
      </div>
      <markdown-editor class="form-text"
                       :id="`approvalRequiredMsg-${skill.skillId}`"
                       ref="approvalRequiredMsg"
                       :project-id="skill.projectId"
                       :skill-id="skill.skillId"
                       data-cy="selfReportMsgInput"
                       :aria-describedby="`reportSkillMsg-${skill.skillId}`"
                       markdownHeight="250px"
                       label="Justification"
                       name="justification"
                       :show-label="false"
                       :aria-label="isJustitificationRequired ? 'Optional request approval justification' : 'Required request approval justification'"
                       :placeholder="`Justification (${isJustitificationRequired ? 'required' : 'optional'})`"
                       :resizable="true"
                       aria-errormessage="approvalMessageError" />

      <div class="text-right mt-2">
        <SkillsButton
          label="Cancel"
          icon="fas fa-times-circle"
          outlined
          size="small"
          severity="warning"
          :disabled="isSubmitting"
          class="uppercase mr-1 skills-theme-btn"
          data-cy="selfReportApprovalCancelBtn"
          @click="cancel" />
        <SkillsButton
          label="Request"
          icon="fas fa-arrow-alt-circle-right"
          severity="success"
          size="small"
          outlined
          class="uppercase skills-theme-btn"
          @click="submit"
          data-cy="selfReportSubmitBtn"
          :disabled="!meta.valid || isSubmitting" />
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>