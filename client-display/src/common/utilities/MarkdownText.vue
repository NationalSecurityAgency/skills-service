<template>
    <span v-html="parseMarkdown(text)"/>
</template>

<script>
    import marked from 'marked';

    export default {
        name: 'MarkdownText',
        props: {
          text: String,
        },
        methods: {
            parseMarkdown(text) {
                return marked(text);
            },
        },
    };
</script>

<style scoped>

</style>
