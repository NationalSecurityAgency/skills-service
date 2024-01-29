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
  cancelButtonSeverity: {
    type: String,
    default: 'warning'
  },
  okButtonDisabled: Boolean,
  enableReturnFocus: {
    type: Boolean,
    default: false
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
</script>

<template>
  <Dialog modal
          @update:visible="onUpdateVisible"
          :closable="!submitting"
          :close-on-escape="!submitting"
          v-model:visible="model"
          :maximizable="true"
          :header="header"
          class="w-11 lg:w-10 xl:w-9"
  >
    <skills-spinner :is-loading="loading" />

    <div v-if="!loading" v-focustrap>
      <slot></slot>

      <div class="text-right">
        <SkillsButton
          label="Cancel"
          icon="far fa-times-circle"
          :severity="cancelButtonSeverity"
          outlined size="small"
          class="float-right mr-2"
          :disabled="submitting"
          @click="onCancel"
          data-cy="closeDialogBtn" />
        <SkillsButton
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