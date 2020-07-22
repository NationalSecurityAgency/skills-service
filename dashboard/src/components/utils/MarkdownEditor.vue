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
    <b-tabs class="h-100">
      <b-tab active>
        <template slot="title">
          <i class="fa fa-edit mr-1"></i> <span>Write</span>
        </template>
        <div class="mt-2 content-height">
          <b-form-textarea rows="5" max-rows="5" v-model="valueInternal" @input="dataChanged"
                           data-cy="markdownEditorInput" no-resize/>
        </div>
      </b-tab>
      <b-tab>
        <template slot="title">
          <i class="fa fa-eye mr-1"></i> <span>Preview</span>
        </template>
        <div class="mt-2 content-height border rounded px-3" style="overflow-y: scroll;">
          <span class="markdown-preview" v-if="valueInternal !== null" v-html="compiledMarkdown"></span>
        </div>
      </b-tab>
    </b-tabs>
    <div><small><b-link to="/markdown" target="_blank">Markdown</b-link> is supported</small></div>
  </div>
</template>

<script>
  import marked from 'marked';
  import debounce from 'lodash.debounce';
  import InputSanitizer from './InputSanitizer';

  export default {
    name: 'MarkdownEditor',
    props: {
      value: String,
    },
    data() {
      return {
        valueInternal: this.value,
      };
    },
    watch: {
      value(newValue) {
        this.valueInternal = newValue;
      },
    },
    computed: {
      compiledMarkdown: function compileMarkdown() {
        if (this.valueInternal) {
          const compiled = InputSanitizer.sanitize(marked(this.valueInternal, { sanitize: true, smartLists: true, gfm: true }));
          return compiled;
        }

        return '';
      },
    },
    methods: {
      dataChanged: debounce(function debouncedDataChanged() {
        this.$emit('input', this.valueInternal);
      }, 250),
    },
  };
</script>

<style>
  .content-height {
    height: 9rem;
  }

  .markdown-preview ul {
    list-style: circle;
  }

  .markdown-preview p {
    margin-top: 1rem;
    margin-bottom: 1rem;
  }

  .markdown-preview blockquote {
    padding: 10px 20px;
    margin: 0 0 20px;
    font-size: 1rem;
    border-left: 5px solid #eeeeee;
    color: #888;
    line-height: 1.5;
  }

  .markdown-preview h1 {
    font-size: 2.5rem;
    font-weight: bold;
  }

  .markdown-preview h2 {
    font-size: 2.2rem;
    font-weight: bold;
  }

  .markdown-preview h3 {
    font-size: 1.9rem;
    font-weight: bold;
    padding-bottom: 5px;
  }

  .markdown-preview h4 {
    font-size: 1.6rem;
    font-weight: bold;
  }

  .markdown-preview h5 {
    font-size: 1.3rem;
    font-weight: bold;
  }

  .markdown-preview h6 {
    font-size: 1rem;
    font-weight: bold;
  }

  .markdown-preview table {
    width: 100%;
  }

  .markdown-preview table thead tr {
    border-bottom: 1px solid #e5e5e5 !important;
    background-color: #eeeeee;
  }

  .markdown-preview table th {
    border: 1px solid #dddddd;
  }

  .markdown-preview table td {
    border: 1px solid #dddddd !important;
  }

  .markdown-preview pre {
    border: 1px solid #dddddd !important;
    margin: 1rem;
    padding: 1rem;
    overflow: auto;
    font-size: 85%;
    border-radius: 6px;
    background-color: #f6f8fa;
  }

</style>
