<script setup>
import { ref, onMounted, watch } from 'vue'
import { useField } from 'vee-validate'
import ToastUiEditor from '@/common-components/utilities/markdown/ToastUiEditor.vue'

import fontSize from 'tui-editor-plugin-font-size'
import "tui-editor-plugin-font-size/dist/tui-editor-plugin-font-size.css";
import { useCommonMarkdownOptions } from '@/common-components/utilities/markdown/UseCommonMarkdownOptions.js'
import { useMarkdownAccessibilityFixes } from '@/common-components/utilities/markdown/UseMarkdownAccessibilityFixes.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useByteFormat } from '@/common-components/filter/UseByteFormat.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import FileUploadService from '@/common-components/utilities/FileUploadService.js'

const appConfig = useAppConfig()

const props = defineProps({
  value: String,
  name: {
    type: String,
    required: true
  },
  resizable: {
    type: Boolean,
    default: false
  },
  allowAttachments: {
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
  projectId: {
    type: String,
    default: null
  },
  quizId: {
    type: String,
    default: null
  },
  skillId: {
    type: String,
    default: null
  },
  disabled: {
    type: Boolean,
    default: false
  },
})

//  build editor options
const toolbarItems = [
  ['heading', 'bold', 'italic', 'strike'],
  ['hr', 'quote'],
  ['ul', 'ol', 'indent', 'outdent'],
  ['image', 'link'],
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
  plugins: [fontSize]
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
  markdownAccessibilityFixes.fixAccessibilityIssues()
}

function onEditorChange() {
  attachmentError.value = ''

  // This looks for an image at the start of the description and adds a newline before it
  // eslint-disable-next-line no-useless-escape
  const imgMatch = /^\!\[.*\]\(.*\)/
  const current = markdownText()
  if (current.match(imgMatch)) {
    moveCursorToStart()
    insertText('\n')
    moveCursorToStart()
  }

  if (props.useHtml) {
    // emit('input', htmlText())
    value.value = htmlText()
  } else {
    // emit('input', markdownText())
    value.value = markdownText()
  }
}

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
      markdownAccessibilityFixes.clickOnHeaderToolbarButton()
    } else if (event.key === 's') {
      markdownAccessibilityFixes.clickOnFontSizeToolbarButton()
    } else if (event.key === 'i') {
      markdownAccessibilityFixes.clickOnImageToolbarButton()
    } else if (event.key === 'r') {
      markdownAccessibilityFixes.clickOnLinkToolbarButton()
    } else if (event.key === 'a') {
      markdownAccessibilityFixes.clickOnAttachmentToolbarButton()
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
        if (props.projectId) {
          data.append('projectId', props.projectId)
        }
        if (props.quizId) {
          data.append('quizId', props.quizId)
        }
        if (props.skillId) {
          data.append('skillId', props.skillId)
        }
        FileUploadService.upload('/api/upload', data, (response) => {
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

watch(value, (newValue) =>{
  if (newValue !== markdownText()) {
    setMarkdownText(newValue)
  }
})
</script>

<template>
  <div id="markdown-editor" @drop="attachFile" class="field text-left" :data-cy="`${name}MarkdownEditor`">
    <label v-if="showLabel"
           data-cy="markdownEditorLabel"
           class="mb-3"
           :class="`${labelClass}`"
           :for="name" @click="focusOnMarkdownEditor">{{ label }}</label>
    <BlockUI :blocked="disabled">
      <toast-ui-editor :id="name"
                       :style="resizable ? {resize: 'vertical', overflow: 'auto'} : {}"
                       class="markdown"
                       data-cy="markdownEditorInput"
                       ref="toastuiEditor"
                       initialEditType="wysiwyg"
                       previewStyle="tab"
                       :initialValue="value"
                       :options="editorOptions"
                       :height="markdownHeight"
                       :disabled="disabled"
                       @change="onEditorChange"
                       @keydown="onKeydown"
                       @focus="handleFocus"
                       @load="onLoad" />
      <div class="editor-help-footer border-1 surface-border border-round-bottom px-2 py-2">
      <div class="flex text-xs">
        <div class="">
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
      <div v-if="attachmentWarningMessage && hasNewAttachment"
           class="p-error text-sm pt-2">
        <i class="fas fa-exclamation-triangle" aria-hidden="true" /> {{ attachmentWarningMessage }}
      </div>
    </div>
    </BlockUI>
    <small role="alert" class="p-error" :id="`${name}Error`" data-cy="descriptionError">{{ errorMessage || '' }}</small>

    <input @change="attachFile"
           type="file"
           ref="fileInputRef"
           aria-label="ability to attach a file"
           aria-errormessage="attachmentError"
           :accept="allowedAttachmentFileTypes"
           hidden />
    <Message severity="error" v-if="attachmentError"
             class="m-0"
             @close="attachmentError = ''"
             role="alert"
             data-cy="attachmentError"
             id="attachmentError">{{ attachmentError }}
    </Message>
  </div>
</template>

<style scoped>
.editor-help-footer {
  border-top: 0.9px dashed rgba(0, 0, 0, 0.2) !important;
  background-color: #f7f9fc !important;
  color: #687278 !important;
}

.editor-help-footer-help-icon {
  font-size: 1rem;
}
</style>

<style>
.markdown .toastui-editor-defaultUI {
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
</style>