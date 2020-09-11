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
    <span class="markdown">
        <span v-html="parseMarkdown(text)"/>
    </span>
</template>

<script>
  import marked from 'marked';
  import emoji from 'node-emoji';
  import DOMPurify from 'dompurify';

  export default {
    name: 'MarkdownText',
    props: {
      text: String,
    },
    methods: {
      parseMarkdown(text) {
        const compiled = marked(text);
        const onMissing = (name) => name;
        const emojified = emoji.emojify(compiled, onMissing);
        const sanitized = DOMPurify.sanitize(emojified);
        return sanitized;
      },
    },
  };
</script>

<style>
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
</style>
