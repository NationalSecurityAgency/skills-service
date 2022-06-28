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
  <div class="container">
    <div id="gfmDescription" class="card">
      <div class="card-header bg-info" @cancel-clicked="$parent.close()">
        <div class="h4 text-white">Markdown Syntax</div>
      </div>

      <div class="card-body">
        <div class="card-text">
          <p class="card-title">Descriptions support GFM (Git Flavored Markdown). Some limited examples of the supported
            formatting are:</p>

          <div class="h3">Headers</div>
          <pre class="bg-light"><code>
        # H1
        ## H2
        ### H3
        #### H4
        ##### H5
        ###### H6
        </code></pre>

          <div class="h3">Emphasis</div>
          <pre class="bg-light"><code>
        italics: *italicized* or _italicized_
        bold: **bolded** or __bolded__
        combination **_bolded & italicized_**
        strikethrough: ~~struck~~
        </code></pre>

          <div class="h3">Inline</div>
          <pre class="bg-light"><code>
        Inline `code` has `back-ticks around` it.
        </code></pre>

          <div class="h3">Multiline</div>
          Use ``` for multiline support:
          <pre class="bg-light"><code>
        ```
        import { SkillsDirective } from '@skilltree/skills-client-vue';
        Vue.use(SkillsDirective);
        ```
          </code></pre>

          <div class="h3">Lists</div>
          <p>Ordered Lists:</p>
          <pre class="bg-light"><code>
        1. Item one
        1. Item two
        1. Item three (actual number does not matter)
        </code></pre>
          <p>
            If List item has multiple lines of text, subsequent lines must be idented four spaces,
            otherwise list item numbers will reset, e.g.,
          </p>
          <pre class="bg-light"><code>
        1. item one
            paragrah one
        1. item two
        1. item three
        </code></pre>
          <p>Unordered Lists</p>
          <pre class="bg-light"><code>
        * Item
        * Item
        * Item
        </code></pre>

          <div class="h3">
            Links
          </div>
          <pre class="bg-light"><code>
        [in line link](https://www.somewebsite.com)
        </code></pre>

          <div class="h3">
            Blockquotes
          </div>
          <pre class="bg-light"><code>
        > This is a block quote
        </code></pre>

          <div class="h3">
            Horizontal rule
          </div>
          <pre class="bg-light"><code>
        Use three or more dashes, asterisks, or underscores to generate a horizontal rule line

        Separate me
        ___
        Separate me
        ---
        Separate me
        ***
        </code></pre>

          <div class="h3">
            Emojis
          </div>
          Emojis are supported, for example:
          <pre class="bg-light"><code>
        :star: :star: :star: :star:

        :squid:  :full_moon:  :gift_heart:
          </code></pre>
          Full list of the supported icons can be found
          <a href="https://raw.githubusercontent.com/omnidan/node-emoji/master/lib/emoji.json" target="_blank">here <i class="fa fa-external-link-alt"/></a>
          <div class="alert alert-warning my-2">
            <i class="fa fa-exclamation-triangle"/> Please note that specific emoji support is unique to each browser.
          </div>

        </div>
      </div>
    </div>
  </div></template>

<script>
  export default {
    name: 'GFMDescription',
    props: [],
    data() {
      return {};
    },
  };
</script>

<style scoped>

</style>
