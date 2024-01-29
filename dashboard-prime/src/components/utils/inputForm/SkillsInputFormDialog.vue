<script setup>
import { ref, reactive, computed } from 'vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import { useForm } from 'vee-validate'
import { useFocusState } from '@/stores/UseFocusState.js'
import { useInputFormResiliency } from '@/components/utils/inputForm/UseInputFormResiliency.js'
import FormReloadWarning from '@/components/utils/inputForm/FormReloadWarning.vue'

const model = defineModel()
const props = defineProps({
  id:{
    type: String,
    required: true
  },
  header: String,
  loading: Boolean,
  saveButtonLabel:{
    type: String,
    default: 'Save'
  },
  validationSchema: Object,
  initialValues: Object,
  enableReturnFocus: {
    type: Boolean,
    default: false
  },
})
const emit = defineEmits(['saved', 'cancelled'])
const { values, errors, meta, handleSubmit, isSubmitting, setFieldValue } = useForm({
  validationSchema: props.validationSchema,
  initialValues: props.initialValues
})
const childBinding = ref({
  formState: meta,
  isSubmitting,
})
const focusState = useFocusState()
const onUpdateVisible = (newVal) => {
  if (!newVal) {
    close()
  }
}

const inputFormResiliency = reactive(useInputFormResiliency())
inputFormResiliency.init(props.id, values, props.initialValues, setFieldValue)

const close =() => {
  model.value = false
  if (props.enableReturnFocus) {
    focusState.focusOnLastElement()
  }
  inputFormResiliency.discard(false)
}
const onSubmit = handleSubmit(values => {
  emit('saved', values);
  close()
})

const isDialogLoading = computed(() => {
  return props.loading || inputFormResiliency.isInitializing
})
</script>

<template>
  <Dialog modal
          @update:visible="onUpdateVisible"
          :closable="!isSubmitting"
          :close-on-escape="!isSubmitting"
          v-model:visible="model"
          :maximizable="true"
          :header="header"
          class="w-11 lg:w-10 xl:w-9"
  >
    <skills-spinner :is-loading="isDialogLoading" />

    <div v-if="!isDialogLoading" v-focustrap>
     <form-reload-warning
       v-if="inputFormResiliency.isRestoredFromStore"
       @discard-changes="inputFormResiliency.discard"/>

      <slot></slot>

      <div class="text-right">
        <SkillsButton
          label="Cancel"
          icon="far fa-times-circle"
          severity="warning"
          outlined size="small"
          class="float-right mr-2"
          :disabled="isSubmitting"
          @click="close"
          data-cy="closeProjectButton" />
        <SkillsButton
          :label="saveButtonLabel"
          icon="far fa-save"
          severity="success"
          outlined size="small"
          class="float-right"
          @click="onSubmit"
          :disabled="!meta.valid || isSubmitting"
          :loading="isSubmitting"
          data-cy="saveProjectButton" />
      </div>
    </div>
  </Dialog>
</template>

<style scoped>

</style>