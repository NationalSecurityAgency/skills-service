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
    <div @click="progressBarClicked">
        <vertical-progress v-if="progress.total === 100"
                           total-progress-bar-color="#59ad52" before-today-bar-color="#59ad52"
                           :total-progress="progress.total"
                           :total-progress-before-today="progress.totalBeforeToday"/>
        <vertical-progress v-if="skill.points !== skill.totalPoints && progress.total !== 100"
                           :total-progress="progress.total"
                           :total-progress-before-today="progress.totalBeforeToday" :is-locked="locked" :is-clickable="isClickable"/>
    </div>
</template>

<script>
    import VerticalProgress from '@/common/progress/VerticalProgress.vue';

    export default {
        name: 'ProgressBar',
        components: {
            VerticalProgress,
        },
        props: {
            skill: Object,
            isClickable: Boolean,
        },
        computed: {
            progress() {
                let totalPts = Math.trunc((this.skill.points / this.skill.totalPoints) * 100);
                // this can happen when project admin adjusts skill definition after the points were achieved.
                if (totalPts > 100) {
                    totalPts = 100;
                }
                return {
                    total: this.isLocked() && totalPts !== 100 ? 0 : totalPts,
                    totalBeforeToday: ((this.skill.points - this.skill.todaysPoints) / this.skill.totalPoints) * 100,
                };
            },
            locked() {
                return this.isLocked();
            },
        },
        methods: {
            progressBarClicked(skill) {
                this.$emit('progressbar-clicked', skill);
            },
            isLocked() {
                return this.skill.dependencyInfo && !this.skill.dependencyInfo.achieved;
            },
        },
    };
</script>

<style scoped>

</style>
