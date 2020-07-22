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
        <h4 class="text-white">Markdown Syntax</h4>
      </div>


      <div class="card-body">
        <div class="card-text">
          <p class="card-title">Descriptions support GFM (Git Flavored Markdown). Some limited examples of the supported
            formatting are:</p>

          <h3>Headers</h3>
          <pre class="bg-light"><code>
        # H1
        ## H2
        ### H3
        #### H4
        ##### H5
        ###### H6
        </code></pre>

          <h3>Emphasis</h3>
          <pre class="bg-light"><code>
        italics: *italicized* or _italicized_
        bold: **bolded** or __bolded__
        combination **_bolded & italicized_**
        strikethrough: ~~struck~~
        </code></pre>

          <h3>Inline</h3>
          <pre class="bg-light"><code>
        Inline `code` has `back-ticks around` it.
        </code></pre>

          <h3>Multiline</h3>
          Use ``` for multiline support:
          <pre class="bg-light"><code>
        ```
        import { SkillsDirective } from '@skilltree/skills-client-vue';
        Vue.use(SkillsDirective);
        ```
          </code></pre>

          <h3>Lists</h3>
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

          <h3>
            Links
          </h3>
          <pre class="bg-light"><code>
        [in line link](https://www.somewebsite.com)
        </code></pre>


          <h3>
            Blockquotes
          </h3>
          <pre class="bg-light"><code>
        > This is a block quote
        </code></pre>

          <h3>
            Horizontal rule
          </h3>
          <pre class="bg-light"><code>
        Use three or more dashes, asterisks, or underscores to generate a horizontal rule line

        Separate me
        ___
        Separate me
        ---
        Separate me
        ***
        </code></pre>
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
