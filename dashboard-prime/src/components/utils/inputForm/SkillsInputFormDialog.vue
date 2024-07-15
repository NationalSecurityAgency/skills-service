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
import { reactive, computed, ref, provide, toRaw, watch } from 'vue'
import { useForm } from 'vee-validate'
import { useInputFormResiliency } from '@/components/utils/inputForm/UseInputFormResiliency.js'
import deepEqual from 'deep-equal';
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
  saveButtonIcon: {
    type: String,
    default: 'far fa-save'
  },
  showSaveButton: {
    type: Boolean,
    default: true
  },
  cancelButtonLabel: {
    type: String,
    default: 'Cancel'
  },
  cancelButtonIcon: {
    type: String,
    default: 'far fa-times-circle'
  },
  cancelButtonSeverity: {
    type: String,
    default: 'warning'
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
  dialogClass: {
    type: String,
    default: 'w-11 xl:w-10'
  },
  enableInputFormResiliency: {
    type: Boolean,
    default: true
  },
  closeOnSuccess: {
    type: Boolean,
    default: true
  },
})
const emit = defineEmits(['saved', 'cancelled', 'isDirty', 'errors'])

const { values, meta, handleSubmit, isSubmitting, setFieldValue, validate, errors } = useForm({
  validationSchema: props.validationSchema,
  initialValues: props.initialValues
})

const inputFormResiliency = reactive(useInputFormResiliency())

const skillsDialog = ref(null)
const cancel = () => {
  emit('cancelled');
  close()
}
const close = () => {
  skillsDialog.value.handleClose()
  model.value = false
  if (props.enableInputFormResiliency) {
    inputFormResiliency.stop()
  }
}
const isSaving = ref(false)
const onSubmit = handleSubmit(formValue => {
  isSaving.value = true
  props.saveDataFunction(formValue).then((res) => {
    if (props.enableInputFormResiliency) {
      inputFormResiliency.stop()
    }
    emit('saved', res)
    if (props.closeOnSuccess) {
      close()
    }
    isSaving.value = false
  })
})

provide('doSubmitForm', onSubmit)
provide('setFieldValue', setFieldValue)

const isDialogLoading = computed(() => {
  return props.loading || (inputFormResiliency.isInitializing && props.enableInputFormResiliency)
})

watch(() => meta.value.dirty, (newValue) => {
  emit('isDirty', newValue)
})
watch(() => errors.value, (newValue) => {
  emit('errors', newValue)
})

const validateIfEditOrNotEmpty = () => {
  const skipAttrs = toRaw(values)['skipTheseAttrsWhenValidatingOnInit'] || []
  if (!props.initialValues) {
    console.error(`Initial values for SkillsInputFormDialog id=[${props.id}] not provided.`)
  }
  if (props.isEdit) {
    validate()
  } else {
    const foundNonEmpty = Object.entries(values)
        .find(([key, value]) =>
            key !== 'skipTheseAttrsWhenValidatingOnInit'
            && !skipAttrs.includes(key)
            && value && !deepEqual((props.initialValues[key]), (value)))
    if (foundNonEmpty) {
      validate()
    }
  }
}

if (props.asyncLoadDataFunction) {
  isLoadingAsyncData.value = true
  props.asyncLoadDataFunction().then((res) => {
    for (const [key, value] of Object.entries(res)) {
      setFieldValue(key, value)
    }
    if (props.enableInputFormResiliency) {
      inputFormResiliency.init(props.id, values, props.initialValues, setFieldValue)
        .then(() => {
          validateIfEditOrNotEmpty()
        })
    } else {
      validateIfEditOrNotEmpty()
    }
  }).finally(() => {
    isLoadingAsyncData.value = false
  })
} else {
  isLoadingAsyncData.value = false
  if (props.enableInputFormResiliency) {
    inputFormResiliency.init(props.id, values, props.initialValues, setFieldValue)
      .then(() => {
        validateIfEditOrNotEmpty()
      })
  } else {
    validateIfEditOrNotEmpty()
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
    @on-cancel="cancel"
    @on-ok="onSubmit"
    :ok-button-label="saveButtonLabel"
    :ok-button-icon="saveButtonIcon"
    :show-ok-button="showSaveButton"
    :ok-button-disabled="!meta.valid || isSubmitting"
    :enable-return-focus="enableReturnFocus"
    :dialog-class="dialogClass"
    :cancel-button-label="cancelButtonLabel"
    :cancel-button-icon="cancelButtonIcon"
    :cancel-button-severity="cancelButtonSeverity"
    :pt="{ content: { class: 'p-0' }, maximizableButton: { 'aria-label': 'Expand to full screen and collapse back to the original size of the dialog' } }"
    footer-class="px-3 pb-3"
  >
    <div class="p-3">
      <form-reload-warning
        v-if="inputFormResiliency.isRestoredFromStore && enableInputFormResiliency"
        @discard-changes="inputFormResiliency.discard" />

      <BlockUI :blocked="isSaving || isSubmitting" :full-screen="false">
        <skills-spinner :is-loading="true" v-if="isSaving || isSubmitting" class="loading-indicator"/>
        <div class="p-2">
          <slot></slot>
        </div>
      </BlockUI>
    </div>
  </SkillsDialog>
</template>

<style scoped>
.loading-indicator {
  position: fixed;
  z-index: 999;
  height: 2em;
  width: 2em;
  overflow: visible;
  margin: auto;
  top: 0;
  left: 0;
  bottom: 0;
  right: 0;
}

</style>