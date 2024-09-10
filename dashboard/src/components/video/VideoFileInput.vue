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
                   :disabled="false"
                   variant="filled"
                   @click="openFileDialog"
                   placeholder="Upload file from my computer by clicking Browse or drag-n-dropping it here..."/>
        <InputGroupAddon class="p-0 m-0">
          <FileUpload
              :pt="{ root: { class: 'border-round-right border-left-none bg-primary' }, input: { id: 'videoFileInput'} }"
              data-cy="videoFileUpload"
              mode="basic"
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
    <div v-if="isInternallyHosted" class="flex align-items-start">
      <InputGroup>
        <InputGroupAddon style="height: 1%">
          <div><i class="fas fa-server mr-1"></i>SkillTree Hosted</div>
        </InputGroupAddon>
        <SkillsTextInput id="videoFileInput"
                         class="flex-1"
                         v-model="props.hostedFileName"
                         data-cy="videoFileInput"
                         name="videoFileInput"
                         :disabled="true"/>
        <SkillsButton
            data-cy="resetBtn"
            size="small"
            style="height: 1%; padding: 0.8rem"
            outlined
            aria-label="Reset Video Upload input option"
            @click="emit('reset')"
            icon="fa fa-broom"
            label="Reset">
        </SkillsButton>
      </InputGroup>
    </div>
  </div>
</template>

<style scoped>

</style>