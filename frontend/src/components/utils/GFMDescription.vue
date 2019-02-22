<template>
    <div id="gfmDescription" class="modal-card" style="width: 1110px;">
      <modal-header title="Markdown Syntax" @cancel-clicked="$parent.close()"></modal-header>


      <section class="modal-card-body content">
        <p>Descriptions support GFM (Git Flavored Markdown). Some limited examples of the supported formatting are:</p>

        <h3>Headers</h3>
        <pre>
        # H1
        ## H2
        ### H3
        #### H4
        ##### H5
        ###### H6
        </pre>

        <h3>Emphasis</h3>
        <pre>
        italics: *italicized* or _italicized_
        bold: **bolded** or __bolded__
        combination **_bolde&italicized_**
        strikethrough: ~~struck~~
        </pre>

        <h3>Inline</h3>
        <pre>
          Inline `code` hs `back-ticks around` it.
        </pre>

        <h3>Lists</h3>
        <p>Ordered Lists:</p>
        <pre>
        1. Item one
        1. Item two
        1. Item three (actual number does not matter)
        </pre>
        <p>
        If  List item has multiple lines of text, subsequent lines must be idented four spaces,
        otherwise list item numbers will reset, e.g.,
        </p>
        <pre>
        1. item one
            paragrah one
        1. item two
        1. item three
        </pre>
        <p>Unordered Lists</p>
        <pre>
        * Item
        * Item
        * Item
        </pre>

        <h3>
        Links
        </h3>
        <pre>
        [in line link](https://www.somewebsite.com)
        </pre>


        <h3>
        Blockquotes
        </h3>
        <pre>
        > This is a block quote
        </pre>

        <h3>
        Horizontal rule
        </h3>
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
  import ModalHeader from './modal/ModalHeader';

  export default {
    name: 'GFMDescription',
    components: { ModalHeader },
    props: [],
    data() {
      return {

      };
    },
  };
</script>

<style scoped>

</style>
