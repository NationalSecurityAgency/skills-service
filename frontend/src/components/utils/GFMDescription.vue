<template>
    <div id="gfmDescription" class="modal-card" style="width: 1110px;">
      <header class="modal-card-head">
        <p class="modal-card-title">Markdown Syntax</p>
        <button class="delete" aria-label="close" v-on:click="$parent.close()"></button>
      </header>

      <section class="modal-card-body">
        <p>Descriptions support GFM (Git Flavored Markdown). Some limited examples of the supported formatting are:</p>

        <p>
        Headers
        </p>
        <pre>
        # H1
        ## H2
        ### H3
        #### H4
        ##### H5
        ###### H6
        </pre>

        <p>
        Emphasis
        </p>
        <pre>
        italics: *italicized* or _italicized_
        bold: **bolded** or __bolded__
        combination **_bolde&italicized_**
        strikethrough: ~~struck~~
        </pre>

        <p>
        Lists
        </p>
        <pre>
        Ordered Lists:
        1. Item one
        1. Item two
        1. Item three (actual number does not matter)

If  List item has multiple lines of text, subsequent lines must be idented four spaces,
otherwise list item numbers will reset, e.g.,
        1. item one
            paragrah one
        1. item two
        1. item three

        Unordered Lists
        * Item
        * Item
        * Item
        </pre>

        <p>
        Links
        <pre>
        [in line link](https://www.somewebsite.com)
        </pre>


        <p>
        Blockquotes
        </p>
        <pre>
        > This is a block quote
        </pre>

        <p>
        Horizontal rule
        </p>
        <pre>
        Use three or more dashes, asterisks, or underscores to generate a horizontal rule line

        Separate me
        ___
        Separate me
        ---
        Separate me
        ***
        </pre>

      </section>
    </div>
</template>

<script>
  export default {
    name: 'GFMDescription',
    props: [],
    data() {
      return {

      };
    },
  };
</script>

<style scoped>

</style>
