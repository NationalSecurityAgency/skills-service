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
    <div class="user-skills-overview">
        <div class="card">
            <div class="row card-body ml-0 mr-0">
                <div class="text-center col-md-4">
                    <circle-progress
                            :total-completed-points="displayData.userSkills.points"
                            :points-completed-today="displayData.userSkills.todaysPoints"
                            :total-possible-points="displayData.userSkills.totalPoints"
                            :completed-before-today-color="beforeTodayColor"
                            :incomplete-color="incompleteColor"
                            :total-completed-color="displayData.userSkills.points === displayData.userSkills.totalPoints ? completeColor : earnedTodayColor"
                            title="Overall Points">
                        <div slot="footer">
                            <p v-if="displayData.userSkills.points > 0 && displayData.userSkills.points === displayData.userSkills.totalPoints">All Points earned</p>
                            <div v-else>
                                <div>Earn up to <strong>{{ displayData.userSkills.totalPoints | number }}</strong> points</div>
                                <div>
                                    <strong>{{ displayData.userSkills.todaysPoints | number }}</strong> Points earned Today
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
                            :completed-before-today-color="beforeTodayColor"
                            :incomplete-color="incompleteColor"
                            :total-completed-color="isLevelComplete ? completeColor : earnedTodayColor"
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
                                    <strong>{{ displayData.userSkills.todaysPoints | number}}</strong> Points earned Today
                                </div>
                            </div>
                        </div>
                    </circle-progress>
                </div>

            </div>
        </div>

        <div class="row pt-3">
            <div class="col-lg-3 pb-3 pb-lg-0">
                <my-rank :display-data="displayData"/>
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
  import PointProgressChart from '@/userSkills/PointProgressChart';

  import CircleProgress from '@/common/progress/CircleProgress';
  import MyRank from '@/userSkills/myRank/MyRank';
  import MySkillLevel from '@/userSkills/MySkillLevel';
  import MyBadges from '@/userSkills/badge/MyBadges';

  export default {
    components: {
      MyBadges,
      PointProgressChart,
      CircleProgress,
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
          title: this.isLevelComplete ? 'Level Progress' : `Level ${this.displayData.userSkills.skillsLevel + 1} Progress`,
          nextLevel: this.displayData.userSkills.skillsLevel + 1,
          pointsTillNextLevel: this.displayData.userSkills.levelTotalPoints - this.displayData.userSkills.levelPoints,
        };
      },
      beforeTodayColor() {
        return this.$store.state.themeModule.progressIndicators.beforeTodayColor;
      },
      earnedTodayColor() {
        return this.$store.state.themeModule.progressIndicators.earnedTodayColor;
      },
      completeColor() {
        return this.$store.state.themeModule.progressIndicators.completeColor;
      },
      incompleteColor() {
        return this.$store.state.themeModule.progressIndicators.incompleteColor;
      },
    },
  };
</script>

<style scoped>

</style>
