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
<script>
const editorEvents = [
  'load',
  'change',
  'focus',
  'keydown',
];
// also available events: 'blur', 'keyup', 'beforeConvertWysiwygToMarkdown', 'beforePreviewRender', 'caretChange',
</script>
<script setup>
import Editor from '@toast-ui/editor';
import { onMounted } from 'vue';

const props = defineProps({
  id: {
    type: String,
    default: 'toastui-editor',
  },
  options: Object,
  initialValue: String,
  initialEditType: {
    type: String,
    default: 'wysiwyg',
  },
  height: {
    type: String,
    default: '300px',
  },
  previewStyle: {
    type: String,
    default: 'tab',
  },
});

const emit = defineEmits(editorEvents);
const eventOptions = {};
editorEvents.forEach((event) => {
  eventOptions[event] = (...args) => {
    emit(event, ...args);
  };
});

const editorContainer = {
  editor: null,
};
onMounted(() => {
  const el = document.querySelector(`#${props.id}`);
  const options = {
    ...props.options,
    initialValue: props.initialValue,
    initialEditType: props.initialEditType,
    previewStyle: props.previewStyle,
    height: props.height,
    events: eventOptions,
    el,
  };
  editorContainer.editor = new Editor(options);
});

function invoke(methodName, ...args) {
  let result = null;

  if (editorContainer.editor[methodName]) {
    result = editorContainer.editor[methodName](...args);
  }

  return result;
}

defineExpose({
  invoke,
});
</script>

<template>
  <div>
    <div :id="id"></div>
  </div>
</template>

<style scoped>

</style>
