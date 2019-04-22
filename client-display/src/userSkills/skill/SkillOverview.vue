<template>
    <div class="card">
        <div class="card-body text-center text-sm-left">
            <div class="row">
                <div class="col">
                    <h4 style="min-width: 10rem;">{{ skill.skill }}</h4>
                </div>
                <div class="col text-sm-right" style="min-width: 10rem;" :class="{ 'text-success' : isSkillComplete }">
                    <span v-if="isSkillComplete" class="pr-1"><i class="fa fa-check"/></span>{{ skill.points }} / {{ skill.totalPoints }} Points
                </div>
            </div>
            <div class="row">
                <div class="col">
                    <progress-bar :skill="skill"/>
                </div>
            </div>

            <skill-summary-cards v-if="!locked" :skill="skill" class="mt-3"></skill-summary-cards>

            <p class="skills-text-description mx-2 mt-3">
                {{ skill.description.description }}
            </p>
        </div>
        <div class="card-footer">
            <div class="row">
                <div v-show="skill.description.href" class="col text-left">
                    <span>Need help?</span>
                    <a :href="skill.description.href" target="_blank">
                        Click here!
                    </a>
                </div>
            </div>
        </div>

    </div>
</template>

<script>
    import ProgressBar from '@/userSkills/skill/ProgressBar.vue';
    import SkillProgressDescription from '@/userSkills/modal/SkillProgressDescription.vue';
    import SkillSummaryCards from '@/userSkills/skill/SkillSummaryCards.vue';

    export default {
        name: 'SkillOverview',
        components: {
            SkillSummaryCards,
            SkillProgressDescription,
            ProgressBar,
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
