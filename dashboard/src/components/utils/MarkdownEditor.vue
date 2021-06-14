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
          <i class="fa fa-edit mr-1" aria-hidden="true"/> <span id="markdownEditLabel">Write</span>
        </template>
        <div class="mt-2" :style="[!resizable ? {'height':markdownHeight} : {}]">
          <b-form-textarea rows="5" max-rows="5" v-model="valueInternal" @input="dataChanged"
                           :no-resize="!resizable"
                           data-cy="markdownEditorInput" aria-labelledby="markdownEditLabel" @mouseup="wasResized"/>
        </div>
      </b-tab>
      <b-tab>
        <template slot="title">
          <i class="fa fa-eye mr-1" aria-hidden="true"/> <span>Preview</span>
        </template>
        <div class="mt-2 border rounded p-3" :style="{'overflow-y':'scroll','height':markdownHeight}" data-cy="markdownEditor-preview">
          <markdown-text v-if="valueInternal" :text="valueInternal"/>
        </div>
      </b-tab>
    </b-tabs>
    <div><small><b-link to="/markdown" target="_blank">Markdown</b-link> is supported</small></div>
  </div>
</template>

<script>
  import debounce from 'lodash.debounce';
  import MarkdownText from './MarkdownText';

  export default {
    name: 'MarkdownEditor',
    components: { MarkdownText },
    props: {
      value: String,
      resizable: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        valueInternal: this.value,
        markdownHeight: '10rem',
      };
    },
    watch: {
      value(newValue) {
        this.valueInternal = newValue;
      },
    },
    methods: {
      dataChanged: debounce(function debouncedDataChanged() {
        this.$emit('input', this.valueInternal);
      }, 250),
      wasResized(e) {
        if (this.resizable) {
          const oneRem = parseFloat(getComputedStyle(document.documentElement).fontSize);
          const targetHeight = e.target.clientHeight;
          const inRem = `${targetHeight / oneRem}rem`;
          if (inRem !== this.markdownHeight) {
            this.markdownHeight = inRem;
          }
        }
      },
    },
  };
</script>

<style scoped>
</style>
