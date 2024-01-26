<script setup>
import { ref, useModel } from 'vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import { useForm } from 'vee-validate'
import { useFocusState } from '@/stores/UseFocusState.js'

const model = defineModel()
const props = defineProps({
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
const { values, errors, meta, handleSubmit, isSubmitting } = useForm({
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
const close =() => {
  model.value = false
  if (props.enableReturnFocus) {
    focusState.focusOnLastElement()
  }
}
const onSubmit = handleSubmit(values => {
  emit('saved', values);
  close()
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
    <skills-spinner :is-loading="loading" />

    <div v-if="!loading" v-focustrap>
      <!--      <ReloadMessage v-if="restoredFromStorage" @discard-changes="discardChanges" />-->

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