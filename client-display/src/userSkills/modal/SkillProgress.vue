<template>
    <div>
        <div class="row my-3 my-md-2 skill-progress">
            <div class="col-sm-12 col-md-3 text-sm-center text-md-right d-inline-block text-truncate">
                <span class="skill-name">{{ skill.skill }}</span>
            </div>
            <div class="col-md-7">
                <progress-bar :skill="skill" @progressbar-clicked="progressBarClicked" :is-clickable="true"/>
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
    import SkillProgressDescription from '@/userSkills/modal/SkillProgressDescription.vue';
    import ProgressBar from '@/userSkills/skill/ProgressBar.vue';


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

    @media screen and (min-width: 768px) {
        .skill-progress .skill-name {
            font-size: 0.75rem;
            font-weight: bold;
        }
    }
</style>
