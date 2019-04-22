<template>
    <div class="row mt-0 px-3 justify-content-center">
        <div class="col-lg-4 mb-2 my-lg-0 px-1">
            <progress-info-card :points="skill.points" label="Overall Points" icon="fa fa-running text-primary"/>
        </div>

        <div class="col-lg-4 mb-2 my-lg-0 px-1">
            <progress-info-card :points="skill.todaysPoints" label="Today's Points"
                                icon="fa fa-clock text-warning"/>
        </div>

        <div class="col-lg-4 m-b2 my-lg-0 px-1">
            <progress-info-card :points="skill.pointIncrement" :label="pointIncrementLabel"
                                icon="fa fa-user-clock text-success"/>
        </div>
    </div>
</template>

<script>
    import ProgressInfoCard from '@/userSkills/modal/ProgressInfoCard.vue';

    export default {
        name: 'SkillSummaryCards',
        components: { ProgressInfoCard },
        props: {
            skill: Object,
        },
        computed: {
            pointIncrementLabel() {
                return `Per ${this.skill.pointIncrementInterval} hours allowance`;
            },
        },
    };
</script>

<style scoped>

</style>
