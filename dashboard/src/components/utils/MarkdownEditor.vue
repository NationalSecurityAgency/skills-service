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
  <div id="markdown-editor">
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
      <div class="row text-secondary small">
        <div class="col">
          Insert images by pasting, dragging & dropping, or selecting from toolbar.
        </div>
        <div class="col-auto">
          <a ref="editorFeatureLinkRef" :href="editorFeaturesUrl" target="_blank" style="display: inline-block"><i class="far fa-question-circle editor-help-footer-help-icon"/></a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import '@toast-ui/editor/dist/toastui-editor.css';
  import { Editor } from '@toast-ui/vue-editor';
  import MarkdownMixin from '@/common-components/utilities/MarkdownMixin';

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
      };
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
    },
    computed: {
      markdownText() {
        return this.$refs.toastuiEditor.invoke('getMarkdown');
      },
      editorFeaturesUrl() {
        return `${this.$store.getters.config.docsHost}/dashboard/user-guide/rich-text-editor.html`;
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
        const options = {
          hideModeSwitch: true,
          usageStatistics: false,
          autofocus: false,
          toolbarItems: [
            ['heading', 'bold', 'italic', 'strike'],
            ['hr', 'quote'],
            ['ul', 'ol', 'indent', 'outdent'],
            ['table', 'image', 'link'],
            ['code', 'codeblock'],
            ['scrollSync'],
          ],
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
    },
  };
</script>

<style scoped>
  .editor-help-footer {
    border-top: 0.9px dashed rgba(0, 0, 0, 0.2) !important;
    background-color: #f7f9fc !important;
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
</style>
