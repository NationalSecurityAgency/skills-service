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
  const el = document.querySelector('#editor');
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
    <div id="editor"></div>
  </div>
</template>

<style scoped>

</style>
