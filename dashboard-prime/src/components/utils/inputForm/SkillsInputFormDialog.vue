<script setup>
import { reactive, computed, ref, provide, toRaw, watch } from 'vue'
import { useForm } from 'vee-validate'
import { useInputFormResiliency } from '@/components/utils/inputForm/UseInputFormResiliency.js'
import FormReloadWarning from '@/components/utils/inputForm/FormReloadWarning.vue'
import SkillsDialog from '@/components/utils/inputForm/SkillsDialog.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'


const isLoadingAsyncData = ref(true)
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
  },
  asyncLoadDataFunction: Function,
  saveDataFunction: {
    type: Function,
    required: true
  },
  isEdit: {
    type: Boolean,
    default: false
  },
})
const emit = defineEmits(['saved', 'cancelled', 'isDirty', 'errors'])

const { values, meta, handleSubmit, isSubmitting, setFieldValue, validate, errors } = useForm({
  validationSchema: props.validationSchema,
  initialValues: props.initialValues
})

const inputFormResiliency = reactive(useInputFormResiliency())

const skillsDialog = ref(null)
const close = () => {
  skillsDialog.value.handleClose()
  model.value = false
  inputFormResiliency.stop()
}
const isSaving = ref(false)
const onSubmit = handleSubmit(formValue => {
  isSaving.value = true
  props.saveDataFunction(formValue).then((res) => {
    inputFormResiliency.stop()
    emit('saved', res)
    close()
    isSaving.value = false
  })
})

provide('doSubmitForm', onSubmit)
provide('setFieldValue', setFieldValue)

const isDialogLoading = computed(() => {
  return props.loading || inputFormResiliency.isInitializing
})

watch(() => meta.value.dirty, (newValue) => {
  emit('isDirty', newValue)
})
watch(() => errors.value, (newValue) => {
  emit('errors', newValue)
})

if (props.asyncLoadDataFunction) {
  isLoadingAsyncData.value = true
  props.asyncLoadDataFunction().then((res) => {
    for (const [key, value] of Object.entries(res)) {
      setFieldValue(key, value)
    }
    inputFormResiliency.init(props.id, values, props.initialValues, setFieldValue)
      .then(() => {
        validateIfNotEmpty()
      })
  }).finally(() => {
    isLoadingAsyncData.value = false
  })
} else {
  isLoadingAsyncData.value = false
  inputFormResiliency.init(props.id, values, props.initialValues, setFieldValue)
    .then(() => {
      validateIfNotEmpty()
    })
}

const validateIfNotEmpty = () => {
  const skipAttrs = toRaw(values)['skipTheseAttrsWhenValidatingOnInit'] || []
  const foundNonEmpty = Object.entries(values)
    .find(([key, value]) =>
      key !== 'skipTheseAttrsWhenValidatingOnInit'
      && !skipAttrs.includes(key)
      && value && props.initialValues[key] !== value)
  if (foundNonEmpty) {
    validate()
  }
}


</script>

<template>
  <SkillsDialog
    ref="skillsDialog"
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

    <div class="loading-indicator" v-if="isSaving || isSubmitting">
      <skills-spinner :is-loading="true"/>
    </div>

    <slot></slot>
  </SkillsDialog>
</template>

<style scoped>
.loading-indicator {
  position: fixed;
  z-index: 999;
  height: 2em;
  width: 2em;
  overflow: show;
  margin: auto;
  top: 0;
  left: 0;
  bottom: 0;
  right: 0;
}

/* Transparent Overlay */
.loading-indicator:before {
  content: '';
  display: block;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(222, 217, 217, 0.53);
}
</style>