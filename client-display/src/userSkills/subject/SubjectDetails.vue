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

            <skills-progress-list :subject="displayData.userSkills" :helpTipHref="displayData.userSkills.helpUrl"/>
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
