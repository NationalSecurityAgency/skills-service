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
    <div class="card">
        <div class="card-body text-center text-sm-left">

            <div v-if="skill.crossProject" class="row border-bottom mb-3 text-primary text-center">
                <div class="col-md-6 text-md-left">
                    <h4><span class="text-muted">Project:</span> {{ skill.projectName }}</h4>
                </div>
                <div class="col-md-6 text-md-right text-success text-uppercase">
                    <h5><i class="fa fa-vector-square"/> Cross-project Skill</h5>
                </div>
            </div>

            <div v-if="skill.crossProject && !isSkillComplete" class="alert alert-primary text-center" role="alert">
                This is a cross-project skill! In order to complete this skill please visit <strong>{{ skill.projectName }}</strong> project! Happy playing!!
            </div>

            <div class="row text-center">
                <div class="col-md-6 text-md-left">
                    <h4>{{ skill.skill }}</h4>
                </div>
                <div class="col-md-6 text-right" :class="{ 'text-success' : isSkillComplete, 'text-primary': !isSkillComplete }">
                    <span v-if="isSkillComplete" class="pr-1"><i class="fa fa-check"/></span>{{ skill.points | number }} / {{ skill.totalPoints | number }} Points
                </div>
            </div>
            <div class="row">
                <div class="col">
                    <progress-bar :skill="skill"/>
                </div>
            </div>

            <skill-summary-cards v-if="!locked" :skill="skill" class="mt-3"></skill-summary-cards>

            <p class="skills-text-description text-primary mx-2 mt-3">
                <markdown-text :text="skill.description.description"/>
            </p>
        </div>
        <div class="card-footer">
            <div class="row">
                <div v-show="skill.description.href" class="col text-left">
                    <span class="text-secondary">Need help?</span>
                    <a :href="skill.description.href" target="_blank" rel="noopener">
                        Click here!
                    </a>
                </div>
            </div>
        </div>

    </div>
</template>

<script>
    import ProgressBar from '@/userSkills/skill/progress/ProgressBar.vue';
    import SkillSummaryCards from '@/userSkills/skill/progress/SkillSummaryCards.vue';
    import MarkdownText from '@/common/utilities/MarkdownText.vue';

    export default {
        name: 'SkillOverview',
        components: {
            SkillSummaryCards,
            ProgressBar,
            MarkdownText,
        },
        props: {
            skill: Object,
        },
        computed: {
            locked() {
                return this.skill.dependencyInfo && !this.skill.dependencyInfo.achieved;
            },
            isSkillComplete() {
                return this.skill.points === this.skill.totalPoints;
            },
        },
    };
</script>

<style scoped>

</style>
