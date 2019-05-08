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
        combination **_bolde&italicized_**
        strikethrough: ~~struck~~
        </code></pre>

          <h3>Inline</h3>
          <pre class="bg-light"><code>
        Inline `code` hs `back-ticks around` it.
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
