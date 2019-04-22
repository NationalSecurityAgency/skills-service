<template>
    <div v-if="loading">
        <vue-simple-spinner size="large" :message="message"/>
    </div>
</template>

<script>
    import Spinner from 'vue-simple-spinner';

    export default {
        name: 'SkillsSpinner',
        components: {
            'vue-simple-spinner': Spinner,
        },
        props: {
            loading: Boolean,
            message: {
                type: String,
                required: false,
                default: 'Loading...',
            },
        },
    };
</script>

<style scoped>

</style>
