<template>
    <div class="row mt-0 px-3 justify-content-center">
        <div class="mb-2 my-lg-0 px-1" :class="{'col-lg-4': !isTimeWindowDisabled, 'col-lg-6': isTimeWindowDisabled}">
            <progress-info-card :points="skill.points" label="Overall Points Earned" icon="fa fa-running text-primary"/>
        </div>

        <div class="mb-2 my-lg-0 px-1" :class="{'col-lg-4': !isTimeWindowDisabled, 'col-lg-6': isTimeWindowDisabled}">
            <progress-info-card :points="skill.todaysPoints" label="Points Achieved Today"
                                icon="fa fa-clock text-warning"/>
        </div>

        <div v-if="!isTimeWindowDisabled" class="col-lg-4 m-b2 my-lg-0 px-1">
            <progress-info-card :points="timeWindowPoints" :label="pointIncrementLabel"
                                icon="fa fa-user-clock text-success"/>
        </div>
    </div>
</template>

<script>
    import ProgressInfoCard from '@/userSkills/skill/progress/ProgressInfoCard.vue';

    export default {
        name: 'SkillSummaryCards',
        components: { ProgressInfoCard },
        props: {
            skill: Object,
        },
        computed: {
            pointIncrementLabel() {
                if (this.skill.totalPoints / this.skill.pointIncrement === 1) {
                    return '1 occurrence will complete the skill.';
                }

                const hours = this.skill.pointIncrementInterval > 59 ? Math.floor(this.skill.pointIncrementInterval / 60) : 0;
                const minutes = this.skill.pointIncrementInterval > 60 ? this.skill.pointIncrementInterval % 60 : this.skill.pointIncrementInterval;
                const occur = this.skill.maxOccurrencesWithinIncrementInterval;
                let res = `${occur} occurrence${this.sOrNothing(occur)} allowed within Time Window of`;
                if (hours) {
                    res = `${res} ${hours} hr${this.sOrNothing(hours)}`;
                }
                if (minutes) {
                    if (hours) {
                        res = ` ${res} and`;
                    }
                    res = `${res} ${minutes} min${this.sOrNothing(minutes)}`;
                }
                return res;
            },
            timeWindowPoints() {
                return this.skill.pointIncrement * this.skill.maxOccurrencesWithinIncrementInterval;
            },
            isTimeWindowDisabled() {
                return this.skill.pointIncrementInterval <= 0;
            },
        },
        methods: {
            sOrNothing(num) {
                return num > 1 ? 's' : '';
            },
        },
    };
</script>

<style scoped>

</style>
