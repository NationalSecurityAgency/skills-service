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
    ></editor>
    <div><small><b-link to="/markdown" target="_blank">Markdown</b-link> is supported</small></div>
  </div>
</template>

<script>
  import '@toast-ui/editor/dist/toastui-editor.css';
  import { Editor } from '@toast-ui/vue-editor';
  import MarkdownMixin from '@/common-components/utilities/MarkdownMixin';
  import emoji from 'node-emoji';

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
        const markdown = this.$refs.toastuiEditor.invoke('getMarkdown');
        return markdown;
      },
      emojiWidgetRule() {
        const reWidgetRule = /([:]\S+[:])/;
        return {
          rule: reWidgetRule,
          toDOM(text) {
            const rule = reWidgetRule;
            const matched = text.match(rule);
            const span = document.createElement('span');
            const onMissing = (name) => name;
            const emojified = emoji.emojify(matched[1], onMissing);
            span.innerHTML = emojified;
            return span;
          },
        };
      },
      editorOptions() {
        const options = {
          hideModeSwitch: false,
          usageStatistics: false,
          autofocus: false,
          widgetRules: [this.emojiWidgetRule],
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
    },
  };
</script>

<style scoped>
  .markdown >>> .toastui-editor-mode-switch .tab-item {
    color: #555 !important;
  }

</style>
