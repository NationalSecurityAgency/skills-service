<script setup>
import { useFocusState } from '@/stores/UseFocusState.js'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'

const model = defineModel()
const props = defineProps({
  header: String,
  loading: {
    type: Boolean,
    default: false,
  },
  submitting: Boolean,
  okButtonLabel: {
    type: String,
    default: 'Ok'
  },
  okButtonIcon: {
    type: String,
    default: 'far fa-save'
  },
  okButtonSeverity: {
    type: String,
    default: 'success'
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
  okButtonDisabled: Boolean,
  showOkButton: {
    type: Boolean,
    default: true,
  },
  enableReturnFocus: {
    type: Boolean,
    default: false
  },
  dialogClass: {
    type: String,
    default: 'w-11 xl:w-10'
  },
  footerClass: {
    type: String,
    default: ''
  },
})
const emit = defineEmits(['on-ok', 'on-cancel'])
const focusState = useFocusState()
const onUpdateVisible = (newVal) => {
  if (!newVal) {
    emit('on-cancel', newVal)
    handleClose()
  }
}

const onCancel = (event) => {
  emit('on-cancel', event)
  handleClose()
}

const onOk = (event) => {
  emit('on-ok', event)
  handleClose()
}

const handleClose = () => {
  if (props.enableReturnFocus) {
    focusState.focusOnLastElement()
  }
}
defineExpose({
  handleClose
})
</script>

<template>
  <Dialog modal
          @update:visible="onUpdateVisible"
          :closable="!submitting"
          :close-on-escape="!submitting"
          v-model:visible="model"
          :maximizable="true"
          :header="header"
          :class="dialogClass"
          :pt="{ maximizableButton: { 'aria-label': 'Expand to full screen and collapse back to the original size of the dialog' } }"
  >
    <skills-spinner :is-loading="loading" />

    <div v-if="!loading" v-focustrap>
      <slot></slot>

      <div :class="`text-right ${footerClass}`">
        <SkillsButton
          :label="cancelButtonLabel"
          :icon="cancelButtonIcon"
          :severity="cancelButtonSeverity"
          outlined size="small"
          class="float-right mr-2"
          :disabled="submitting"
          @click="onCancel"
          data-cy="closeDialogBtn" />
        <SkillsButton
          v-if="showOkButton"
          :label="okButtonLabel"
          :icon="okButtonIcon"
          :severity="okButtonSeverity"
          outlined size="small"
          class="float-right"
          @click="onOk"
          :disabled="okButtonDisabled || submitting"
          :loading="submitting"
          data-cy="saveDialogBtn" />
      </div>
    </div>
  </Dialog>
</template>

<style scoped>

</style>