<template>
  <b-tabs type="is-boxed">
    <b-tab-item>
      <template slot="header">
        <i class="fa fa-edit"></i> <span>Write</span>
      </template>
      <div class="markdown-input-container title">
        <textarea class="textarea markdown-input" :value="valueInternal" @input="updateValue"></textarea>
      </div>
    </b-tab-item>
    <b-tab-item>
      <template slot="header">
        <i class="fa fa-eye"></i> <span>Preview</span>
      </template>
      <div class="markdown-preview-container table">
        <span class="markdown-preview" v-if="valueInternal !== null" v-html="compiledMarkdown"></span>
      </div>
    </b-tab-item>
  </b-tabs>
</template>

<script>
  import marked from 'marked';
  import debounce from 'lodash.debounce';

  export default {
    name: 'MarkdownEditor',
    props: ['value'],
    data() {
      return {
        valueInternal: this.value,
      };
    },
    computed: {
      compiledMarkdown: function compileMarkdown() {
        if (this.valueInternal) {
          const compiled = marked(this.valueInternal, { sanitize: true, smartLists: true, gfm: true });
          return compiled;
        }

        return '';
      },
    },
    methods: {
      updateValue: debounce(function handleInputEvent(e) {
        this.valueInternal = e.target.value;
        const event = { value: this.valueInternal, markedDown: this.compiledMarkdown };
        this.$emit('value-updated', event);
      }, 300),
    },
  };
</script>

<style>
  .markdown-input-container {
    min-height: 10rem;
  }
  .markdown-input {
    height: 10rem;
  }
  .markdown-preview-container {
    border: 1px dashed darkgray;
    padding: 1rem;
    min-height: 10rem;
  }
  .markdown-preview {
    /*all: unset;*/
    /*margin: 2rem;*/
  }
  .markdown-preview ul {
    list-style: circle;
    margin-left: 2rem;
  }
  .markdown-preview ol {
    margin-left: 2rem;
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

</style>
