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
import {computed, onMounted, ref} from 'vue'
import {useField} from 'vee-validate'
import ToastUiEditor from '@/common-components/utilities/markdown/ToastUiEditor.vue'

import fontSize from 'tui-editor-plugin-font-size'
import "tui-editor-plugin-font-size/dist/tui-editor-plugin-font-size.css";
import {useCommonMarkdownOptions} from '@/common-components/utilities/markdown/UseCommonMarkdownOptions.js'
import {useMarkdownAccessibilityFixes} from '@/common-components/utilities/markdown/UseMarkdownAccessibilityFixes.js'
import {useSkillsAnnouncer} from '@/common-components/utilities/UseSkillsAnnouncer.js'
import {useByteFormat} from '@/common-components/filter/UseByteFormat.js'
import {useAppConfig} from '@/common-components/stores/UseAppConfig.js'
import FileUploadService from '@/common-components/utilities/FileUploadService.js'
import {useThemesHelper} from '@/components/header/UseThemesHelper.js'
import {useDebounceFn} from '@vueuse/core'
import {useLog} from "@/components/utils/misc/useLog.js";

const appConfig = useAppConfig()
const log = useLog()

const props = defineProps({
  value: String,
  name: {
    type: String,
    required: true
  },
  id: {
    type: String,
    required: false,
  },
  resizable: {
    type: Boolean,
    default: false
  },
  allowAttachments: {
    type: Boolean,
    default: true
  },
  allowInsertImages: {
    type: Boolean,
    default: true
  },
  label: {
    type: String,
    default: 'Description'
  },
  showLabel: {
    type: Boolean,
    default: true
  },
  labelClass: {
    type: String,
    default: ''
  },
  markdownHeight: {
    type: String,
    default: '300px'
  },
  placeholder: {
    type: String,
    default: ''
  },
  useHtml: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  uploadUrl: {
    type: String,
    default: null
  },
})
const emit = defineEmits(['value-changed'])
const themeHelper = useThemesHelper()
const idForToastUIEditor = props.id || props.name
//  build editor options
const toolbarItems = [
  ['heading', 'bold', 'italic', 'strike'],
  ['hr', 'quote'],
  ['ul', 'ol', 'indent', 'outdent'],
  props.allowInsertImages ? ['image', 'link'] : ['link'],
  ['code', 'codeblock'],
  ['scrollSync']
]
if (props.allowAttachments) {
  toolbarItems[3].splice(3, 0, {
    name: 'attachFile',
    tooltip: 'Attach files',
    command: 'attachFile',
    className: 'attachment-button toastui-editor-toolbar-icons fa fa-paperclip',
    style: { backgroundImage: 'none' }
  })
}
const options = {
  hideModeSwitch: true,
  usageStatistics: false,
  autofocus: false,
  placeholder: props.placeholder,
  toolbarItems,
  plugins: [fontSize],
}
const announcer = useSkillsAnnouncer()
const commonOptions = useCommonMarkdownOptions()
const markdownAccessibilityFixes = useMarkdownAccessibilityFixes()
const editorOptions = Object.assign(commonOptions.markdownOptions, options)

const { value, errorMessage } = useField(() => props.name)
if (value === undefined || value == null) {
  value.value =  ''
}
const toastuiEditor = ref(null)
const fileInputRef = ref(null)
const attachmentError = ref('')

const moveCursorToStart = () => {
  return toastuiEditor.value.invoke('moveCursorToStart')
}
const insertText = (newValue) => {
  return toastuiEditor.value.invoke('insertText', newValue)
}
const markdownText = () => {
  return toastuiEditor.value.invoke('getMarkdown')
}
const htmlText = () => {
  return toastuiEditor.value.invoke('getHTML')
}
const focusOnMarkdownEditor = () => {
  return toastuiEditor.value.invoke('focus')
}
const addFileLink = (linkUrl, linkText) => {
  toastuiEditor.value.invoke('exec', 'addLink', { linkUrl, linkText })
}
const setMarkdownText = (newText) => {
  return toastuiEditor.value.invoke('setMarkdown', newText)
}

onMounted(() => {
  if (props.allowAttachments) {
    toastuiEditor.value.invoke('addCommand', 'wysiwyg', 'attachFile', () => {
      fileInputRef.value.click();
    });
  }
})
function onLoad() {
  markdownAccessibilityFixes.fixAccessibilityIssues(idForToastUIEditor, props.allowInsertImages)
}

const updateValue = () => {
  if (props.useHtml) {
    value.value = htmlText()
  } else {
    value.value = markdownText()
  }
}
const imgMatch = /^\!\[.*\]\(.*\)/

const handleOnChange = () => {
  attachmentError.value = ''

  const startTime = performance.now();
  updateValue()
  const endTime = performance.now();
  const totalMs = endTime - startTime;

  // This looks for an image at the start of the description and adds a newline before it
  const maxLenToConsiderInsertingNewLine = 100000
  if (value.value && value.value.length < maxLenToConsiderInsertingNewLine && value.value.match(imgMatch)) {
    moveCursorToStart()
    insertText('\n')
    moveCursorToStart()
    updateValue()
  }

  const minLenToConsiderDebounce = 10000
  if (value.value?.length < minLenToConsiderDebounce) {
    if (onChangeFunc.value !== withoutDebounce) {
      log.info(`Message is too small, change handleOnChange back to default - no debounce`)
      onChangeFunc.value = withoutDebounce
    }
  } else if ( value.value?.length > minLenToConsiderDebounce) {
    const functionCandidate = debounceBasedOnRetrievingTextLatency(totalMs)
    if (functionCandidate.func !== onChangeFunc.value) {
      log.info(`Change handleOnChange function to ${functionCandidate.message}`)
      onChangeFunc.value = functionCandidate.func
    }
  }
  emit('value-changed', value.value)
}
const debounceBasedOnRetrievingTextLatency = (latencyInMs) => {
  if (latencyInMs < 10) {
    return { func: withoutDebounce, message: 'no debounce' }
  } else if (latencyInMs < 30) {
    return { func: debounce200, message: '200ms debounce' }
  } else if (latencyInMs < 50) {
    return { func: debounce400, message: '400ms debounce' }
  } else if (latencyInMs < 80) {
    return { func: debounce600, message: '600ms debounce' }
  } else if (latencyInMs < 120) {
    return { func: debounce800, message: '800ms debounce' }
  }
  return { func: debounce1000, message: '1s debounce' }
}
const withoutDebounce = handleOnChange
const debounce200 = useDebounceFn(handleOnChange, 200)
const debounce400 = useDebounceFn(handleOnChange, 400)
const debounce600 = useDebounceFn(handleOnChange, 600)
const debounce800 = useDebounceFn(handleOnChange, 800)
const debounce1000 = useDebounceFn(handleOnChange, 1000)
const onChangeFunc = ref(withoutDebounce)

const editorFeatureLinkRef = ref(null)
function onKeydown(mode, event) {
  const eventType = event.key.toUpperCase()
  if (eventType === 'TAB' || eventType === 'ESCAPE') {
    if (!event.shiftKey) {
      event.stopPropagation()
      editorFeatureLinkRef?.value?.focus({ focusVisible: true })
    }
  } else if (event.ctrlKey && event.altKey && !event.shiftKey) {
    if (event.key === 't') {
      markdownAccessibilityFixes.clickOnHeaderToolbarButton(idForToastUIEditor)
    } else if (event.key === 's') {
      markdownAccessibilityFixes.clickOnFontSizeToolbarButton(idForToastUIEditor)
    } else if (event.key === 'i') {
      markdownAccessibilityFixes.clickOnImageToolbarButton(idForToastUIEditor)
    } else if (event.key === 'r') {
      markdownAccessibilityFixes.clickOnLinkToolbarButton(idForToastUIEditor)
    } else if (event.key === 'a') {
      markdownAccessibilityFixes.clickOnAttachmentToolbarButton(idForToastUIEditor)
    }
  }
}

function handleFocus() {
  if (props.label) {
    announcer.polite(props.label)
  }
}

const allowedAttachmentFileTypes = appConfig.allowedAttachmentFileTypes
const maxAttachmentSize = appConfig.maxAttachmentSize
const attachmentWarningMessage = appConfig.attachmentWarningMessage
const allowedAttachmentMimeTypes = appConfig.allowedAttachmentMimeTypes

const hasNewAttachment = ref(false)
const byteFormat = useByteFormat()
function attachFile(event) {
  if (!props.allowAttachments) {
    return
  }
  const files = event?.dataTransfer?.files ? event?.dataTransfer?.files : event?.target?.files
  if (files && files.length > 0) {
    const file = [...files].find((el) => allowedAttachmentMimeTypes.some((type) => el.type.indexOf(type) !== -1))
    event.preventDefault()
    event.stopPropagation()
    if (file) {
      hasNewAttachment.value = true
      attachmentError.value = '' // reset any previous error
      if (file.size <= maxAttachmentSize) {
        const data = new FormData()
        data.append('file', file)
        FileUploadService.upload(props.uploadUrl, data, (response) => {
          addFileLink(response.data.href, response.data.filename)
          fileInputRef.value.value = ''
        }, (err) => {
          const explanation = err?.response?.data?.explanation
          if (explanation) {
            attachmentError.value = `Error uploading file [${file.name}] - ${err?.response?.data?.explanation}`
          } else {
            attachmentError.value = `Error uploading file [${file.name}] - ${err?.message}`
          }
        })
      } else {
        attachmentError.value = `Unable to upload attachment - File size [${byteFormat.prettyBytes(file.size)}] exceeds maximum file size [${byteFormat.prettyBytes(maxAttachmentSize)}]`
      }
    } else {
      attachmentError.value = `Unable to upload attachment - File type is not supported. Supported file types are [${allowedAttachmentFileTypes}]`
    }
  }
}
const editorStyle = computed(() => {
  if (!props.resizable) {
    return {}
  }
  return {
    resize: 'vertical',
    overflow: 'auto',
    'min-height': '285px'
  }
})
</script>

<template>
  <div id="markdown-editor" @drop="attachFile" class="flex flex-col gap-2 text-left" :data-cy="`${name}MarkdownEditor`">
    <label v-if="showLabel"
           data-cy="markdownEditorLabel"
           :class="`${labelClass}`"
           :for="name" @click="focusOnMarkdownEditor">{{ label }}</label>
    <BlockUI :blocked="disabled">

      <toast-ui-editor :id="idForToastUIEditor"
                       :style="editorStyle"
                       class="no-bottom-border"
                       :class="{'editor-theme-dark' : themeHelper.isDarkTheme, 'is-resizable': resizable }"
                       data-cy="markdownEditorInput"
                       ref="toastuiEditor"
                       initialEditType="wysiwyg"
                       previewStyle="tab"
                       :initialValue="value"
                       :options="editorOptions"
                       :height="markdownHeight"
                       :disabled="disabled"
                       @change="onChangeFunc"
                       @keydown="onKeydown"
                       @focus="handleFocus"
                       @load="onLoad" />
      <div class="border border-surface bg-surface-100 dark:bg-surface-700 rounded-b px-2 py-2 sd-theme-tile-background">
      <div  class="flex text-xs">
        <div v-if="allowInsertImages" class="">
          Insert images and attach files by pasting, dragging & dropping, or selecting from toolbar.
        </div>
        <div class="flex-1 text-right">
          <a data-cy="editorFeaturesUrl" ref="editorFeatureLinkRef"
             aria-label="SkillTree documentation of rich text editor features"
             :href="`${appConfig.docsHost}/dashboard/user-guide/rich-text-editor.html`"
             target="_blank">
            <i class="far fa-question-circle editor-help-footer-help-icon" />
          </a>
        </div>
      </div>
      <Message
        v-if="attachmentWarningMessage && hasNewAttachment"
        severity="error"
        data-cy="attachmentWarningMessage"
        class="m-0 mt-2">
        {{ attachmentWarningMessage }}
      </Message>
    </div>
    </BlockUI>
    <Message severity="error"
             variant="simple"
             size="small"
             :closable="false"
             data-cy="descriptionError"
             :id="`${name}Error`">{{ errorMessage || '' }}</Message>

    <input @change="attachFile"
           type="file"
           ref="fileInputRef"
           aria-label="ability to attach a file"
           aria-errormessage="attachmentError"
           :accept="allowedAttachmentFileTypes"
           hidden />
    <Message severity="error" v-if="attachmentError"
             class="m-0 mt-2"
             @close="attachmentError = ''"
             role="alert"
             data-cy="attachmentError"
             id="attachmentError">{{ attachmentError }}
    </Message>
  </div>
</template>

<style scoped>

.editor-help-footer-help-icon {
  font-size: 1rem;
}

</style>

<style>
.is-resizable > div {
  height: 100% !important;
  min-height: 238px !important;
}

.is-resizable .toastui-editor-defaultUI > .toastui-editor-main,
.is-resizable .toastui-editor-defaultUI > .toastui-editor-main > .toastui-editor-main-container,
.is-resizable .toastui-editor-defaultUI > .toastui-editor-main > .toastui-editor-main-container > .toastui-editor-ww-container > .toastui-editor {
  min-height: 238px !important;
}

.editor-theme-dark .toastui-editor-ww-container {
  background-color: #1f2937 !important;
}
.editor-theme-dark .toastui-editor-defaultUI-toolbar {
  background-color: #374151 !important;
}
.editor-theme-dark .toastui-editor-ww-code-block pre,
.editor-theme-dark .toastui-editor-contents code {
  background-color: #374151 !important;
}
.editor-theme-dark .toastui-editor-popup-body {
  background-color: #1f2937 !important;
}
.editor-theme-dark .toastui-editor-popup-body li:hover {
  background-color: #41444a !important;
  color: #dadde6 !important;
}
.editor-theme-dark .toastui-editor-popup-body input,
.editor-theme-dark .toastui-editor-popup-add-image .toastui-editor-file-name.has-file {
  color: #dadde6 !important;
}
.editor-theme-dark .toastui-editor-contents p,
.editor-theme-dark .toastui-editor-contents hr,
.editor-theme-dark .toastui-editor-popup-add-image .toastui-editor-tabs .tab-item,
.editor-theme-dark .toastui-editor-popup-body label,
.editor-theme-dark .toastui-editor-ww-code-block pre,
.editor-theme-dark .toastui-editor-contents code,
.editor-theme-dark .toastui-editor-contents h1,
.editor-theme-dark .toastui-editor-contents h2,
.editor-theme-dark .toastui-editor-contents h3,
.editor-theme-dark .toastui-editor-contents h4,
.editor-theme-dark .toastui-editor-contents h5,
.editor-theme-dark .toastui-editor-contents h6 {
  color: rgba(255, 255, 255, 0.87) !important;
}

.editor-theme-dark .toastui-editor-toolbar-group button {
  background-color: #c2ccda !important;
  color: #454545 !important;
}
.editor-theme-dark span.placeholder.ProseMirror-widget {
  color: #d5d5d5 !important;
}

.editor-theme-dark .toastui-editor-toolbar-icons {
  color: #c2ccda !important;
}
.editor-theme-dark .toastui-editor-defaultUI {
  border: 1px solid #424b57
}

.no-bottom-border .toastui-editor-defaultUI {
  border-bottom: none !important;
  border-bottom-left-radius: 0 !important;
  border-bottom-right-radius: 0 !important;
}

div.toastui-editor-ww-code-block:after {
  content: none !important;
}

.attachment-button {
  font-size: 1.1rem !important;
  color: #6c6c6c !important;
  background-image: none !important;
}

span.placeholder.ProseMirror-widget {
  color: #687278 !important;
}

div.toastui-editor-contents {
  font-size: 0.9rem !important;
}
.toastui-editor-defaultUI-toolbar {
  flex-wrap: wrap;
}

.toastui-editor .toastui-editor-contents  blockquote p {
  color: #636363;
}
</style>