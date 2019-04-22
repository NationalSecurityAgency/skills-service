<template>
    <div class="container">
        <skills-spinner :loading="loading" />

        <div v-if="!loading">
            <skills-title>Badge Details</skills-title>

            <div class="card">
                <div class="card-body">
                    <badge-details-overview :badge="badge"></badge-details-overview>
                </div>
            </div>

            <skills-progress-list v-if="badge" :subject="badge" :show-descriptions="showDescriptions" :helpTipHref="helpTipHref"/>
        </div>
    </div>
</template>

<script>
    import BadgeDetailsOverview from '@/userSkills/badge/BadgeDetailsOverview.vue';
    import SkillsProgressList from '@/userSkills/skill/progress/SkillsProgressList.vue';
    import SkillsSpinner from '@/common/utilities/SkillsSpinner.vue';

    import UserSkillsService from '@/userSkills/service/UserSkillsService';

    import 'vue-popperjs/dist/vue-popper.css';
    import SkillsTitle from '@/common/utilities/SkillsTitle.vue';

    export default {
        components: {
            SkillsTitle,
            SkillsProgressList,
            BadgeDetailsOverview,
            SkillsSpinner,
        },
        data() {
            return {
                loading: true,
                badge: null,
                initialized: false,
                showDescriptions: false,
            };
        },
        computed: {
            helpTipHref() {
                return 'http://url';
            },
        },
        watch: {
            $route: 'fetchData',
        },
        mounted() {
            this.fetchData();
        },
        methods: {
            fetchData() {
                UserSkillsService.getBadgeSkills(this.$route.params.badgeId)
                    .then((badgeSummary) => {
                        this.badge = badgeSummary;
                        this.loading = false;
                    });
            },
        },
    };
</script>

<style scoped>

</style>
