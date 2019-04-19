<template>
    <div>
        <div class="row my-3 my-md-2 skill-progress">
            <div class="col-sm-12 col-md-3 text-sm-center text-md-right d-inline-block text-truncate">
                <span class="skill-name">{{ skill.skill }}</span>
            </div>
            <div class="col-md-7">
                <div v-on:click="progressBarClicked">
                    <vertical-progress v-if="progress.total === 100"
                                       total-progress-bar-color="#59ad52" before-today-bar-color="#59ad52"
                                       :total-progress="progress.total"
                                       :total-progress-before-today="progress.totalBeforeToday"/>
                    <vertical-progress v-if="skill.points !== skill.totalPoints && progress.total !== 100"
                                       :total-progress="progress.total"
                                       :total-progress-before-today="progress.totalBeforeToday" :is-locked="locked"/>
                </div>
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
    import VerticalProgress from '@/common/progress/VerticalProgress.vue';
    import SkillProgressDescription from '@/userSkills/modal/SkillProgressDescription.vue';


    export default {
        name: 'UserSkillProgress',
        components: {
            SkillProgressDescription,
            VerticalProgress,
        },
        props: {
            skill: Object,
            showDescription: Boolean,
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

<style>
    .skill-progress .skill-name {
        font-size: 1.1rem;
    }

    @media screen and (min-width: 768px) {
        .skill-progress .skill-name {
            font-size: 0.75rem;
            font-weight: bold;
        }
    }
</style>
