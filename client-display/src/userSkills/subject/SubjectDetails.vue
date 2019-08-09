<template>
    <section class="container">
        <skills-spinner :loading="loading.userSkills"></skills-spinner>

        <div v-if="!loading.userSkills"
             class="user-skill-subject-body">
            <skills-title>{{ displayData.userSkills.subject }}</skills-title>

            <div class="user-skill-subject-overall">
                <user-skills-header :display-data="displayData"/>
            </div>

            <div v-if="displayData.userSkills.description" class="card mt-2">
                <div class="card-header">
                    <h6 class="card-title mb-0 float-left">Description</h6>
                </div>
                <div class="card-body">
                    <markdown-text :text="displayData.userSkills.description" class="font-italic d-block text-left"/>
                </div>
            </div>

            <skills-progress-list :subject="displayData.userSkills"/>
        </div>
    </section>
</template>

<script>
    import UserSkillsHeader from '@/userSkills/UserSkillsHeader.vue';
    import SkillDisplayDataLoadingMixin from '@/userSkills/SkillDisplayDataLoadingMixin.vue';
    import SkillsTitle from '@/common/utilities/SkillsTitle.vue';
    import SkillsProgressList from '@/userSkills/skill/progress/SkillsProgressList.vue';
    import SkillsSpinner from '@/common/utilities/SkillsSpinner.vue';
    import MarkdownText from '@/common/utilities/MarkdownText.vue';

    export default {
        mixins: [SkillDisplayDataLoadingMixin],
        components: {
            MarkdownText,
            UserSkillsHeader,
            SkillsTitle,
            SkillsProgressList,
            SkillsSpinner,
        },
        watch: {
            $route: 'fetchData',
        },
        computed: {
            helpTipHref() {
                return '';
            },
        },
        mounted() {
            this.fetchData();
        },
        methods: {
            fetchData() {
                this.resetLoading();
                this.loadSubject();
                this.loadUserSkillsRanking();
                this.loadPointsHistory();
            },
        },
    };
</script>

<style>

</style>
