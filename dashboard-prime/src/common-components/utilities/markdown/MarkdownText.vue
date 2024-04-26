<script setup>
import { ref, watch } from 'vue'
import { useCommonMarkdownOptions } from '@/common-components/utilities/markdown/UseCommonMarkdownOptions.js'
import ToastUiViewer from '@/common-components/utilities/markdown/ToastUiViewer.vue'

const props = defineProps({
  text: String,
  markdownHeight: {
    type: String,
    default: '300px',
  },
  instanceId: {
    type: String,
    default: '1'
  }
})

const commonOptions = useCommonMarkdownOptions()
const viewerOptions = commonOptions.markdownOptions
const toastuiViewer = ref(null)

watch(() => props.text, (newValue) => {
  setMarkdownText(newValue)
});

const setMarkdownText = (newText) => {
  return toastuiViewer.value.invoke('setMarkdown', newText)
}
</script>

<template>
  <span class="markdown">
      <toast-ui-viewer :id="`toastuiViewer-${instanceId}`"
                       :instance-id="instanceId"
                       data-cy="markdownViewer"
                       ref="toastuiViewer"
                       :initialValue="text"
                       :options="viewerOptions"
                       :height="markdownHeight" />
  </span>
</template>

<style scoped>
.markdown div.toastui-editor-contents p {
  color: inherit !important;
}

.markdown blockquote {
  padding: 10px 20px;
  margin: 0 0 20px;
  font-size: 1rem;
  border-left: 5px solid #eeeeee;
  color: #888;
  line-height: 1.5;
}

.markdown pre {
  border: 1px solid #dddddd !important;
  margin: 1rem;
  padding: 1rem;
  overflow: auto;
  font-size: 85%;
  border-radius: 6px;
  background-color: #f6f8fa;
}
.markdown a {
  text-decoration: underline;
}
</style>