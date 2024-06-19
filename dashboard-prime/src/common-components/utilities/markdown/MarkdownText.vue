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