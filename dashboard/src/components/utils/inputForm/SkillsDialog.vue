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
import { onMounted, onBeforeUnmount } from 'vue';
import { useFocusState } from '@/stores/UseFocusState.js'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'

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
    default: 'warn'
  },
  okButtonDisabled: Boolean,
  showOkButton: {
    type: Boolean,
    default: true,
  },
  showCancelButton: {
    type: Boolean,
    default: true,
  },
  enableReturnFocus: {
    type: Boolean,
    default: false
  },
  dialogClass: {
    type: String,
    default: 'w-11/12 xl:w-10/12'
  },
  footerClass: {
    type: String,
    default: ''
  },
  shouldConfirmCancel: {
    type: Boolean,
    default: false
  },
})
const emit = defineEmits(['on-ok', 'on-cancel', 'confirm-cancel'])
const focusState = useFocusState()

onMounted(() => {
  window.addEventListener('keydown', handleEscape);
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleEscape)
})


const themeHelper = useThemesHelper()

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

const handleEscape = (event) => {
  if (event.key === 'Escape' && !props.submitting) {
    emit('confirm-cancel', event)
    handleClose()
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
          :close-on-escape="!shouldConfirmCancel && !submitting"
          v-model:visible="model"
          :maximizable="false"
          :header="header"
          :class="dialogClass"
  >
    <skills-spinner :is-loading="loading" />

    <div v-if="!loading" v-focustrap :class="{ 'st-dark-theme': themeHelper.isDarkTheme, 'st-light-theme': !themeHelper.isDarkTheme }">
      <slot></slot>

      <div :class="`text-right ${footerClass}`" class="flex justify-end">
        <SkillsButton
            v-if="showCancelButton"
            :label="cancelButtonLabel"
            :icon="cancelButtonIcon"
            :severity="cancelButtonSeverity"
            outlined
            class="float-right mr-2"
            :disabled="submitting"
            @click="onCancel"
            data-cy="closeDialogBtn" />
        <SkillsButton
            v-if="showOkButton"
            :label="okButtonLabel"
            :icon="okButtonIcon"
            :severity="okButtonSeverity"
            outlined
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