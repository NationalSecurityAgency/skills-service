<template>
    <div class="py-2 mt-2 mb-3 card-body skills-page-title-text-color card rounded bg-white text-info text-uppercase">
        <div v-if="backButton" class="position-absolute">
            <h2><button @click="navigateBack" type="button" class="btn btn-outline-info" data-cy="back">
                    <i class="fas fa-arrow-left"></i>
                </button>
            </h2>
        </div>

        <h2 class="ml-5">
            <slot/>
        </h2>
    </div>
</template>

<script>
    export default {
        name: 'SkillsTitle',
        props: {
            backButton: { type: Boolean, default: true },
        },
        methods: {
            navigateBack() {
                this.$router.go(-1);
            },
        },
    };
</script>

<style>
    .skills-page-title-text-color button {
        color: white;
        border-color: white;
    }
    .skills-page-title-text-color button:hover {
        color: #626d7d;
        background-color: white;
        border-color: white;
    }
</style>
