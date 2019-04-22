<template>
    <div v-on:click="progressBarClicked">
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
                return {
                    total: (this.skill.points / this.skill.totalPoints) * 100,
                    totalBeforeToday: ((this.skill.points - this.skill.todaysPoints) / this.skill.totalPoints) * 100,
                };
            },
            locked() {
                return this.skill.dependencyInfo && !this.skill.dependencyInfo.achieved;
            },
        },
        methods: {
            progressBarClicked(skill) {
                this.$emit('progressbar-clicked', skill);
            },
        },
    };
</script>

<style scoped>

</style>
