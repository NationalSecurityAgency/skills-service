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

import { onUpdated } from 'vue';
import FileUpload from 'primevue/fileupload';
import SkillsTextInput from '@/components/utils/inputForm/SkillsTextInput.vue';
import SkillsButton from '@/components/utils/inputForm/SkillsButton.vue';

const emit = defineEmits(['file-selected', 'reset'])
const props = defineProps({
  isInternallyHosted: Boolean,
  showFileUpload: Boolean,
  hostedFileName: String,
  name: {
    type: String,
    required: true,
  },
  disabled: {
    type: Boolean,
    required: false
  },
})

onUpdated(() => {
  setupVideoFileDropTarget();
});

const setupVideoFileDropTarget = () => {
  const videoFileDropTarget = document.getElementById('videoFileInputDropTarget');
  if (videoFileDropTarget) {
    videoFileDropTarget.addEventListener('dragover', (event) => {
      event.preventDefault();
    });
    videoFileDropTarget.addEventListener('dragenter', (event) => {
      event.preventDefault();
    });
    videoFileDropTarget.addEventListener('drop', (event) => {
      event.stopPropagation()
      event.preventDefault();
      selectFile(event.dataTransfer.files[0], true);
    });
  }
}

const onFileSelectedEvent = (selectEvent) => {
  selectFile(selectEvent.files[0]);
}
const selectFile = (file, isDropped = false) => {
  emit('file-selected', { file, isDropped });
}
const openFileDialog = (event) => {
  const videoFileInput = document.getElementById('videoFileInput');
  if (videoFileInput) {
    videoFileInput.click();
  }
}
</script>

<template>
  <div>
    <div v-if="showFileUpload && !isInternallyHosted">
      <InputGroup>
        <InputText :pt="{ root: { readOnly: true } }"
                   id="videoFileInputDropTarget"
                   data-cy="videoFileInputDropTarget"
                   :disabled="disabled"
                   variant="filled"
                   @click="openFileDialog"
                   placeholder="Upload file from my computer by clicking Browse or drag-n-dropping it here..."/>
        <InputGroupAddon>
          <FileUpload
              :pt="{ root: { class: 'border-round-right border-l-0 bg-primary' }, input: { id: 'videoFileInput'} }"
              data-cy="videoFileUpload"
              mode="basic"
              :disabled="disabled"
              :auto="true"
              :show-upload-button="false"
              :custom-upload="true"
              @uploader=""
              @select="onFileSelectedEvent"
              chooseLabel="Browse"/>
        </InputGroupAddon>
      </InputGroup>
    </div>

    <!-- file chosen or already uploaded and internally hosted via SkillTree -->
    <div v-if="isInternallyHosted" class="flex items-start">
      <InputGroup>
        <SkillsTextInput class="flex-1 rounded-none"
                         v-model="props.hostedFileName"
                         data-cy="videoFileInput"
                         name="videoFileInput"
                         :disabled="true">
          <template #addOnBefore><label class="text-surface-600 dark:text-surface-200" for="videoFileInput"><i class="fas fa-server mr-1"></i>SkillTree Hosted</label></template>
          <template #addOnAfter><SkillsButton
              data-cy="resetBtn"
              aria-label="Reset Upload input option"
              @click="emit('reset')"
              icon="fa fa-broom"
              :outlined="false"
              :disabled="disabled"
              severity="secondary"
              label="Reset">
          </SkillsButton></template>
        </SkillsTextInput>

      </InputGroup>
    </div>
  </div>
</template>

<style scoped>

</style>