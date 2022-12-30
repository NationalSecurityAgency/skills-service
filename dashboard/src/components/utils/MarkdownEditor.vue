/*
Copyright 2020 SkillTree

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
<template>
  <div id="markdown-editor" @drop="attachFile">
    <editor :style="resizable ? {resize: 'vertical', overflow: 'auto'} : {}"
      class="markdown"
      data-cy="markdownEditorInput"
      ref="toastuiEditor"
      initialEditType="wysiwyg"
      previewStyle="tab"
      :initialValue="valueInternal"
      :options="editorOptions"
      :height="markdownHeight"
      @change="onEditorChange"
      @keydown="handleTab"
    ></editor>
    <div class="editor-help-footer border px-3 py-2 rounded-bottom">
      <div class="row small">
        <div class="col">
          Insert images by pasting, dragging & dropping, or selecting from toolbar.
        </div>
        <div class="col-auto">
          <a data-cy="editorFeaturesUrl" ref="editorFeatureLinkRef"
             aria-label="SkillTree documentation of rich text editor features"
             :href="editorFeaturesUrl" target="_blank" style="display: inline-block">
            <i class="far fa-question-circle editor-help-footer-help-icon"/>
          </a>
        </div>
      </div>
    </div>

    <ValidationProvider
      rules="attachmentValidator" ref="provider" v-slot="{ errors }" name="Attachment">
      <input @change="attachFile" type="file" ref="fileInputRef"
             :aria-invalid="errors && errors.length > 0"
             aria-errormessage="attachmentError"
             aria-describedby="attachmentError"
             :accept="allowedAttachmentFileTypes"
             hidden/>
      <small role="alert" class="form-text text-danger" data-cy="attachmentError" id="attachmentError">{{errors[0]}}</small>
    </ValidationProvider>
  </div>
</template>

<script>
  import { extend } from 'vee-validate';
  import '@toast-ui/editor/dist/toastui-editor.css';
  import { Editor } from '@toast-ui/vue-editor';
  import fontSize from 'tui-editor-plugin-font-size';
  import 'tui-editor-plugin-font-size/dist/tui-editor-plugin-font-size.css';
  import MarkdownMixin from '@/common-components/utilities/MarkdownMixin';
  import FileUploadService from './upload/FileUploadService';

  export default {
    name: 'MarkdownEditor',
    components: { Editor },
    mixins: [MarkdownMixin],
    props: {
      value: String,
      resizable: {
        type: Boolean,
        default: false,
      },
      allowAttachments: {
        type: Boolean,
        default: true,
      },
      name: {
        type: String,
        default: 'Description',
      },
      markdownHeight: {
        type: String,
        default: '300px',
      },
    },
    data() {
      return {
        valueInternal: this.value,
        intervalId: null,
        intervalRuns: 0,
        maxIntervalAttempts: 8,
        attachmentError: '',
      };
    },
    created() {
      const self = this;
      extend('attachmentValidator', {
        message: () => this.attachmentError,
        validate() {
          return {
            required: false,
            valid: !self.attachmentError,
          };
        },
        computesRequired: true,
      });
    },
    mounted() {
      this.intervalId = setInterval(() => {
        this.intervalRuns += 1;
        if (this.intervalRuns <= this.maxIntervalAttempts) {
          this.setLabelForMoreButton();
        } else {
          clearInterval(this.intervalId);
        }
      }, 250);
      this.setLabelForMoreButton();
      if (this.allowAttachments) {
        this.$refs.toastuiEditor.invoke('addCommand', 'wysiwyg', 'attachFile', () => {
          this.$refs.fileInputRef.click();
        });
      }
    },
    computed: {
      markdownText() {
        return this.$refs.toastuiEditor.invoke('getMarkdown');
      },
      editorFeaturesUrl() {
        return `${this.$store.getters.config.docsHost}/dashboard/user-guide/rich-text-editor.html`;
      },
      maxAttachmentSize() {
        return this.$store.getters.config.maxAttachmentSize;
      },
      allowedAttachmentFileTypes() {
        return this.$store.getters.config.allowedAttachmentFileTypes;
      },
      allowedAttachmentMimeTypes() {
        return this.$store.getters.config.allowedAttachmentMimeTypes;
      },
      // see: https://github.com/NationalSecurityAgency/skills-service/issues/1714
      // emojiWidgetRule() {
      //   const reWidgetRule = /([:]\S+[:])/;
      //   return {
      //     rule: reWidgetRule,
      //     toDOM(text) {
      //       const rule = reWidgetRule;
      //       const matched = text.match(rule);
      //       const span = document.createElement('span');
      //       const onMissing = (name) => name;
      //       const emojified = emoji.emojify(matched[1], onMissing);
      //       span.innerHTML = emojified;
      //       return span;
      //     },
      //   };
      // },
      editorOptions() {
        const toolbarItems = [
          ['heading', 'bold', 'italic', 'strike'],
          ['hr', 'quote'],
          ['ul', 'ol', 'indent', 'outdent'],
          ['table', 'image', 'link'],
          ['code', 'codeblock'],
          ['scrollSync'],
        ];
        if (this.allowAttachments) {
          toolbarItems[3].splice(3, 0, {
            name: 'attachFile',
            tooltip: 'Attach files',
            command: 'attachFile',
            className: 'attachment-button toastui-editor-toolbar-icons fa fa-paperclip',
            style: { backgroundImage: 'none' },
          });
        }
        const options = {
          hideModeSwitch: true,
          usageStatistics: false,
          autofocus: false,
          toolbarItems,
          plugins: [fontSize],
          // widgetRules: [this.emojiWidgetRule],
        };
        return Object.assign(this.markdownOptions, options);
      },
    },
    methods: {
      onEditorChange() {
        this.$emit('input', this.markdownText);
      },
      setLabelForMoreButton() {
        this.$nextTick(() => {
          const toolbarElem = document.getElementsByClassName('more toastui-editor-toolbar-icons')[0];
          if (toolbarElem) {
            toolbarElem.setAttribute('aria-label', 'More');
          }
        });
      },
      handleTab(mode, event) {
        const eventType = event.key.toUpperCase();
        if (eventType === 'TAB' || eventType === 'ESCAPE') {
          if (!event.shiftKey) {
            event.stopPropagation();
            this.$refs.editorFeatureLinkRef?.focus({ focusVisible: true });
          }
        }
      },
      attachFile(event) {
        const files = event?.dataTransfer?.files ? event?.dataTransfer?.files : event?.target?.files;
        if (files && files.length > 0) {
          const file = [...files].find((el) => this.allowedAttachmentMimeTypes.some((type) => el.type.indexOf(type) !== -1));
          if (file) {
            event.preventDefault();
            event.stopPropagation();
            this.attachmentError = ''; // reset any previous error
            if (file.size <= this.maxAttachmentSize) {
              const data = new FormData();
              data.append('file', file);
              FileUploadService.upload('/api/upload', data, (response) => {
                this.$refs.toastuiEditor.invoke('exec', 'addLink', {
                  linkUrl: response.data.href,
                  linkText: response.data.filename,
                });
                this.$refs.fileInputRef.value = '';
              }, (err) => {
                const explanation = err?.response?.data?.explanation;
                if (explanation) {
                  this.attachmentError = `Error uploading file [${file.name}] - ${err?.response?.data?.explanation}`;
                } else {
                  this.attachmentError = `Error uploading file [${file.name}] - ${err?.message}`;
                }
                this.$refs.provider.validate(event);
              });
            } else {
              this.attachmentError = `Unable to upload attachment - File size [${file.size}] exceeds maximum file size [${this.maxAttachmentSize}]`;
            }
          } else {
            this.attachmentError = `Unable to upload attachment - Invalid file type [${[...files][0].type}]`;
          }
          this.$refs.provider.validate(event);
        }
      },
    },
  };
</script>

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
</style>
