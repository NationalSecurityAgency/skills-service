<template>
    <div class="py-2 mt-2 mb-3 card-body skills-page-title-text-color card rounded bg-white text-info text-uppercase">
        <div v-if="!isHomePage" class="position-fixed">
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
        methods: {
            navigateBack() {
                this.$router.go(-1);
            },
        },
        computed: {
            isHomePage() {
                return this.$route.path === '/';
            },
        },
    };
</script>
