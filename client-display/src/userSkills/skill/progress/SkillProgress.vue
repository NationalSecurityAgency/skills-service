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
    <div data-cy="skillProgress">
        <div class="row my-3 my-md-2 skill-progress">
            <div class="col-sm-12 col-md-3 text-sm-center text-md-right d-inline-block text-truncate">
                <span class="skill-name" data-toggle="tooltip" :title="skill.skill" @click="progressBarClicked(skill)" data-cy="skillProgressTitle">{{ skill.skill }}</span>
            </div>
            <div class="col-md-7">
                <progress-bar :skill="skill" @progressbar-clicked="progressBarClicked" :is-clickable="true" class="skills-navigable-item" data-cy="skillProgressBar"/>
            </div>
            <div class="col-sm-12 col-md-2 text-right text-md-center">
                <small>{{ skill.points | number }} / {{ skill.totalPoints | number }}</small>
                <i v-if="skill.points === skill.totalPoints" class="fa fa-check item-complete-icon ml-1"/>
            </div>
        </div>
        <div class="row">
            <div class="col-md-3"></div>
            <div class="col-12 col-md-7" v-if="showDescription">
                <skill-progress-description v-if="showDescription" :skill="skill" :locked="locked"/>
            </div>
        </div>
    </div>
</template>

<script>
    import SkillProgressDescription from '@/userSkills/skill/progress/SkillProgressDescription.vue';
    import ProgressBar from '@/userSkills/skill/progress/ProgressBar.vue';


    export default {
        name: 'SkillProgress',
        components: {
            ProgressBar,
            SkillProgressDescription,
        },
        props: {
            skill: Object,
            showDescription: Boolean,
        },
        methods: {
            progressBarClicked(skill) {
                this.$emit('progressbar-clicked', skill);
            },
        },
        computed: {
            locked() {
                return this.skill.dependencyInfo && !this.skill.dependencyInfo.achieved;
            },
        },
    };
</script>

<style>
    .skill-progress .skill-name {
        font-size: 1.1rem;
    }

    .skill-progress .skill-name:hover {
        text-decoration: underline;
        cursor: pointer;
    }

    @media screen and (min-width: 768px) {
        .skill-progress .skill-name {
            font-size: 0.75rem;
            font-weight: bold;
        }
    }
</style>
