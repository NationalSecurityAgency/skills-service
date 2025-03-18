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
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'

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
const themeHelper = useThemesHelper()
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
                       :class="{'markdown-text-theme-dark' : themeHelper.isDarkTheme }"
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

<style>
.markdown-text-theme-dark pre,
.markdown-text-theme-dark code {
  background-color: #374151 !important;
}
.markdown-text-theme-dark .toastui-editor-contents table tbody tr td,
.markdown-text-theme-dark .toastui-editor-contents h1,
.markdown-text-theme-dark .toastui-editor-contents h2,
.markdown-text-theme-dark .toastui-editor-contents h3,
.markdown-text-theme-dark .toastui-editor-contents h4,
.markdown-text-theme-dark .toastui-editor-contents h5,
.markdown-text-theme-dark .toastui-editor-contents h6,
.markdown-text-theme-dark .toastui-editor-contents hr,
.markdown-text-theme-dark .toastui-editor-contents p,
.markdown-text-theme-dark code,
.markdown-text-theme-dark pre {
  color: rgba(255, 255, 255, 0.87) !important;
}

.markdown .toastui-editor-contents  blockquote p {
  color: #636363;
}
</style>