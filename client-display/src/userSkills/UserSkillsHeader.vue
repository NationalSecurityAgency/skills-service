<template>
    <div class="user-skills-overview">
        <div class="card">
            <div class="row card-body">
                <div class="text-center col-md-4">
                    <circle-progress
                            :user-skills="displayData.userSkills"
                            :total-completed-points="displayData.userSkills.points"
                            :points-completed-today="displayData.userSkills.todaysPoints"
                            :total-possible-points="displayData.userSkills.totalPoints"
                            :total-completed-color="displayData.userSkills.points === displayData.userSkills.totalPoints ? '#59ad52' : '#7ed6f3'"
                            title="Overall Points">
                        <div slot="footer">
                            <p v-if="displayData.userSkills.points === displayData.userSkills.totalPoints">Total points earned</p>
                            <div v-else>
                                <div>Earn up to <strong>{{ displayData.userSkills.totalPoints | number }}</strong> points</div>
                                <div>
                                    <strong>{{ displayData.userSkills.todaysPoints }}</strong> Points earned Today
                                </div>
                            </div>
                        </div>
                    </circle-progress>
                </div>

                <div class="text-center col-md-4 my-5 my-md-0">
                    <my-skill-level :skill-level="displayData.userSkills.skillsLevel" :total-num-levels="displayData.userSkills.totalLevels"/>
                </div>

                <div class="text-center col-md-4">
                    <circle-progress
                            :total-completed-points="displayData.userSkills.levelPoints"
                            :points-completed-today="displayData.userSkills.todaysPoints"
                            :total-possible-points="displayData.userSkills.levelTotalPoints"
                            :total-completed-color="isLevelComplete ? '#59ad52' : '#7ed6f3'"
                            :title="levelStats.title">
                        <div slot="footer">
                            <p v-if="isLevelComplete">All levels complete</p>

                            <div v-if="!isLevelComplete">
                                <div>
                                    <strong>{{ levelStats.pointsTillNextLevel | number }}</strong>
                                    {{ 'Point' | plural(levelStats.pointsTillNextLevel) }} to Level {{
                                    levelStats.nextLevel }}
                                </div>
                                <div>
                                    <strong>{{ displayData.userSkills.todaysPoints }}</strong> Points earned Today
                                </div>
                            </div>
                        </div>
                    </circle-progress>
                </div>

            </div>
        </div>

        <div class="row pt-3">
            <div class="col-lg-3 pb-3 pb-lg-0">
                <my-rank v-if="displayData.userSkillsRanking"
                        :rank="displayData.userSkillsRanking.position"
                        :subject="displayData.userSkills.subject"/>
            </div>

            <div id="point-progress-container" class="pb-3 pb-lg-0"
                    :class="{ 'col-lg-6' : hasBadges, 'col-lg-9' : !hasBadges }">
                <point-progress-chart :points-history="displayData.pointsHistory"/>
            </div>

            <div v-if="hasBadges" class="col-lg-3">
                <my-badges :num-badges-completed="displayData.userSkills.badges.numBadgesCompleted"></my-badges>
            </div>
        </div>
    </div>
</template>

<script>
    import MyProgressSummary from '@/userSkills/MyProgressSummary.vue';
    import PointProgressChart from '@/userSkills/PointProgressChart.vue';

    import CircleProgress from '@/common/progress/CircleProgress.vue';
    import StarProgress from '@/common/progress/StarProgress.vue';
    import MyRank from '@/userSkills/myRank/MyRank.vue';
    import MySkillLevel from '@/userSkills/MySkillLevel.vue';
    import MyBadges from '@/userSkills/badge/MyBadges.vue';

    export default {
        components: {
            MyBadges,
            PointProgressChart,
            MyProgressSummary,
            CircleProgress,
            StarProgress,
            MyRank,
            MySkillLevel,
        },
        props: {
            displayData: Object,
            maxLevel: {
                type: Number,
                default: 5,
            },
        },
        computed: {
            isLevelComplete() {
                return this.displayData.userSkills.levelTotalPoints === -1;
            },
            hasBadges() {
                return this.displayData.userSkills && this.displayData.userSkills.badges && this.displayData.userSkills.badges.enabled;
            },
            levelStats() {
                return {
                    title: `Level ${this.displayData.userSkills.skillsLevel + 1} Progress`,
                    nextLevel: this.displayData.userSkills.skillsLevel + 1,
                    pointsTillNextLevel: this.displayData.userSkills.levelTotalPoints - this.displayData.userSkills.levelPoints,
                };
            },
        },
    };
</script>

<style scoped>

</style>
