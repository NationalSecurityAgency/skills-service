<script setup>
import { ref, reactive, computed } from 'vue'
import { useForm } from 'vee-validate'
import { useInputFormResiliency } from '@/components/utils/inputForm/UseInputFormResiliency.js'
import FormReloadWarning from '@/components/utils/inputForm/FormReloadWarning.vue'
import SkillsDialog from '@/components/utils/inputForm/SkillsDialog.vue'

const model = defineModel()
const props = defineProps({
  id: {
    type: String,
    required: true
  },
  header: String,
  loading: Boolean,
  saveButtonLabel: {
    type: String,
    default: 'Save'
  },
  validationSchema: Object,
  initialValues: Object,
  enableReturnFocus: {
    type: Boolean,
    default: false
  }
})
const emit = defineEmits(['saved', 'cancelled'])

const { values, errors, meta, handleSubmit, isSubmitting, setFieldValue } = useForm({
  validationSchema: props.validationSchema,
  initialValues: props.initialValues
})

const inputFormResiliency = reactive(useInputFormResiliency())
inputFormResiliency.init(props.id, values, props.initialValues, setFieldValue)

const close = () => {
  model.value = false
  inputFormResiliency.discard(false)
}
const onSubmit = handleSubmit(formValue => {
  emit('saved', formValue)
  close()
})



const isDialogLoading = computed(() => {
  return props.loading || inputFormResiliency.isInitializing
})
</script>

<template>
  <SkillsDialog
    v-model="model"
    :header="header"
    :loading="isDialogLoading"
    :submitting="isSubmitting"
    @on-cancel="close"
    @on-ok="onSubmit"
    :ok-button-label="saveButtonLabel"
    ok-button-icon="far fa-save"
    :ok-button-disabled="!meta.valid || isSubmitting"
    :enable-return-focus="enableReturnFocus"
  >
    <form-reload-warning
      v-if="inputFormResiliency.isRestoredFromStore"
      @discard-changes="inputFormResiliency.discard" />


    <slot></slot>
  </SkillsDialog>
</template>

<style scoped>

</style>