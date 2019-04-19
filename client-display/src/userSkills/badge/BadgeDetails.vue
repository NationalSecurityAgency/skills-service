<template>
    <div class="container">
        <ribbon v-if="badge">
            {{ badge.badge }}
        </ribbon>

        <div class="card">
            <div class="card-body">
                <badge-details-overview :badge="badge"></badge-details-overview>
            </div>
        </div>

        <skills-progress-list v-if="badge" :subject="badge" :show-descriptions="showDescriptions" :helpTipHref="helpTipHref"/>
    </div>
</template>

<script>
    import Ribbon from '@/common/ribbon/Ribbon.vue';
    import BadgeDetailsOverview from '@/userSkills/badge/BadgeDetailsOverview.vue';
    import SkillsProgressList from '@/userSkills/modal/SkillsProgressList.vue';


    import UserSkillsService from '@/userSkills/service/UserSkillsService';

    import 'vue-popperjs/dist/vue-popper.css';

    export default {
        components: {
            Ribbon,
            SkillsProgressList,
            BadgeDetailsOverview,
        },
        data() {
            return {
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
                    });
            },
        },
    };
</script>

<style scoped>

</style>
