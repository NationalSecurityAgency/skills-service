<template>
    <span v-html="parseMarkdown(text)"/>
</template>

<script>
    import marked from 'marked';
    import DOMPurify from 'dompurify';

    export default {
        name: 'MarkdownText',
        props: {
          text: String,
        },
        methods: {
            parseMarkdown(text) {
                return DOMPurify.sanitize(marked(text));
            },
        },
    };
</script>

<style scoped>

</style>
