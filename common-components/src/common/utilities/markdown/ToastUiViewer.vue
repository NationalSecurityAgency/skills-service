<script setup>
import '@toast-ui/editor/dist/toastui-editor-viewer.css';
import Viewer from '@toast-ui/editor/dist/toastui-editor-viewer';
import { onMounted } from 'vue';

const props = defineProps({
  options: Object,
  initialValue: String,
  height: {
    type: String,
    default: '300px',
  },
  instanceId: {
    type: String,
    default: '1'
  }
});

const viewerContainer = {
  viewer: null,
};
onMounted(() => {
  const el = document.querySelector(`#viewer-${props.instanceId}`);
  const options = {
    ...props.options,
    initialValue: props.initialValue,
    height: props.height,
    el,
  };
  viewerContainer.viewer = new Viewer(options);
});

function invoke(methodName, ...args) {
  let result = null;

  if (viewerContainer.viewer[methodName]) {
    result = viewerContainer.viewer[methodName](...args);
  }

  return result;
}

defineExpose({
  invoke,
});
</script>

<template>
  <div>
    <div :id="`viewer-${instanceId}`"></div>
  </div>
</template>

<style scoped>

</style>
