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
import {reactive, computed, ref, provide, toRaw, watch, nextTick, onUnmounted, onMounted} from 'vue'
import { useForm } from 'vee-validate'
import { useInputFormResiliency } from '@/components/utils/inputForm/UseInputFormResiliency.js'
import deepEqual from 'deep-equal';
import FormReloadWarning from '@/components/utils/inputForm/FormReloadWarning.vue'
import SkillsDialog from '@/components/utils/inputForm/SkillsDialog.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";
import {useDialogUtils} from "@/components/utils/inputForm/UseDialogUtils.js";
import MatomoEvents from "@/utils/MatomoEvents.js";
import {useMatomoSupport} from "@/stores/UseMatomoSupport.js";

onUnmounted(() => {
  inputFormResiliency.stop(false);
})

const matomo = useMatomoSupport()
const dialogMessages = useDialogMessages()
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
    default: 'warn'
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
  isCopy: {
    type: Boolean,
    default: false
  },
  dialogClass: {
    type: String,
    default: 'w-11/12 xl:w-10/12'
  },
  enableInputFormResiliency: {
    type: Boolean,
    default: true
  },
  closeOnSuccess: {
    type: Boolean,
    default: true
  },
  shouldConfirmCancel: {
    type: Boolean,
    default: false,
  }
})
const emit = defineEmits(['saved', 'cancelled', 'isDirty', 'errors'])

const { values, meta, handleSubmit, isSubmitting, setFieldValue, validate, errors, resetForm } = useForm({
  validationSchema: props.validationSchema,
  initialValues: props.initialValues
})

const inputFormResiliency = reactive(useInputFormResiliency())

onMounted(() => {
  matomo.trackEvent(MatomoEvents.category.Modal, MatomoEvents.action.Open, props.header)
})
const skillsDialog = ref(null)
const cancel = () => {
  emit('cancelled');
  matomo.trackEvent(MatomoEvents.category.Modal, MatomoEvents.action.Cancel, props.header)
  close()
}
const confirmCancel = () => {
  if(meta.value.dirty) {
    dialogMessages.msgConfirm({
      targetElement: skillsDialog.value,
      message: 'You have unsaved changes.  Discard?',
      header: 'Discard Unsaved Changes',
      acceptLabel: 'Discard',
      rejectLabel: 'Cancel',
      accept: () => {
        emit('cancelled');
        matomo.trackEvent(MatomoEvents.category.Modal, MatomoEvents.action.Cancel, props.header)
        close()
      },
      onShowHandler: () => {
        setTimeout(() => {
          nextTick(() => {
            const discardButton = document.querySelector('[aria-label="Discard"]')
            if (discardButton) {
              discardButton.focus()
            } else {
              console.warn('SkillsInputFormDialog" Failed to focus on discard button')
            }
          })
        }, 600)
      }
    });
  } else {
    emit('cancelled');
    matomo.trackEvent(MatomoEvents.category.Modal, MatomoEvents.action.Cancel, props.header)
    close()
  }
}
const close = () => {
  skillsDialog.value.handleClose()
  model.value = false
  if (props.enableInputFormResiliency) {
    inputFormResiliency.stop()
  }
}
const isSaving = ref(false)
const failed = ref(false)
const failedInfo = ref({})
const onSubmit = handleSubmit(formValue => {
  isSaving.value = true
  props.saveDataFunction(formValue).then((res) => {
    if (res?.failed) {
      failed.value = true
      failedInfo.value = res
    } else {
      if (props.enableInputFormResiliency) {
        inputFormResiliency.stop()
      }
      emit('saved', res)
      matomo.trackEvent(MatomoEvents.category.Modal, MatomoEvents.action.Save, props.header)
      if (props.closeOnSuccess) {
        close()
      }
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
  if (props.isEdit || props.isCopy) {
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
  props.asyncLoadDataFunction().then(() => {
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

watch(() => props.initialValues, (newValues) => {
  resetForm({values: newValues});
})

const dialogUtils = useDialogUtils()

defineExpose({
  setFieldValue,
  validate
})
</script>

<template>
  <SkillsDialog
    ref="skillsDialog"
    v-model="model"
    :header="header"
    :loading="isDialogLoading"
    :submitting="isSubmitting || isSaving"
    :shouldConfirmCancel="shouldConfirmCancel"
    @confirm-cancel="confirmCancel"
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
    :pt="{ content: { class: 'p-0' }, pcMaximizeButton: dialogUtils.getMaximizeButtonPassThrough() }"
    footer-class="px-4 pb-4"
  >
    <div v-if="!failed" class="p-4">
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
    <div v-if="failed">
      <slot name="failed" :failedInfo="failedInfo"></slot>
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