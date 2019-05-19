<template>
  <div id="markdown-editor">
    <b-tabs class="h-100">
      <b-tab active>
        <template slot="title">
          <i class="fa fa-edit mr-1"></i> <span>Write</span>
        </template>
        <div class="mt-2 content-height">
          <b-form-textarea rows="5" max-rows="5" v-model="valueInternal" @input="dataChanged" no-resize/>
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
          const compiled = marked(this.valueInternal, { sanitize: true, smartLists: true, gfm: true });
          return compiled;
        }

        return '';
      },
    },
    methods: {
      dataChanged: debounce(function debouncedCataChanged(event) {
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
