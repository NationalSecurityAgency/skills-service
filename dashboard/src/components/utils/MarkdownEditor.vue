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
      data-cy="markdownEditorInput"
      ref="toastuiEditor"
      initialEditType="wysiwyg"
      :initialValue="valueInternal"
      :options="editorOptions"
      :height="markdownHeight"
      @change="onEditorChange"
    ></editor>
  </div>
</template>

<script>
  import '@toast-ui/editor/dist/toastui-editor.css';
  import { Editor } from '@toast-ui/vue-editor';

  export default {
    name: 'MarkdownEditor',
    components: { Editor },
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
    },
    data() {
      return {
        valueInternal: this.value,
        markdownHeight: '15rem',
        editorOptions: {
          hideModeSwitch: false,
          usageStatistics: false,
        },
      };
    },
    watch: {
      value(newValue) {
        this.valueInternal = newValue;
      },
    },
    computed: {
      markdownText() {
        const markdown = this.$refs.toastuiEditor.invoke('getMarkdown');
        return markdown;
      },
    },
    methods: {
      onEditorChange() {
        this.$emit('input', this.markdownText);
      },
    },
  };
</script>

<style scoped>
</style>
