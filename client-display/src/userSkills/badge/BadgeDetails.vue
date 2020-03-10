/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
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

            <skills-progress-list v-if="badge" :subject="badge" :show-descriptions="showDescriptions" :helpTipHref="helpTipHref" type="badge"/>
        </div>
    </div>
</template>

<script>
    import BadgeDetailsOverview from '@/userSkills/badge/BadgeDetailsOverview.vue';
    import SkillsProgressList from '@/userSkills/skill/progress/SkillsProgressList.vue';
    import SkillsSpinner from '@/common/utilities/SkillsSpinner.vue';

    import UserSkillsService from '@/userSkills/service/UserSkillsService';

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
                return this.badge ? this.badge.helpUrl : '';
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
