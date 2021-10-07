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
                            :total-completed-points="displayDataInternal.userSkills.points"
                            :points-completed-today="displayDataInternal.userSkills.todaysPoints"
                            :total-possible-points="displayDataInternal.userSkills.totalPoints"
                            :completed-before-today-color="beforeTodayColor"
                            :incomplete-color="incompleteColor"
                            :total-completed-color="displayDataInternal.userSkills.points === displayDataInternal.userSkills.totalPoints ? completeColor : earnedTodayColor"
                            title="Overall Points">
                        <div slot="footer">
                            <p v-if="displayDataInternal.userSkills.points > 0 && displayDataInternal.userSkills.points === displayDataInternal.userSkills.totalPoints">All Points earned</p>
                            <div v-else>
                                <div>Earn up to <strong>{{ displayDataInternal.userSkills.totalPoints | number }}</strong> points</div>
                                <div>
                                    <strong>{{ displayDataInternal.userSkills.todaysPoints | number }}</strong> Points earned Today
                                </div>
                            </div>
                        </div>
                    </circle-progress>
                </div>

                <div class="text-center col-md-4 my-5 my-md-0">
                    <my-skill-level :skill-level="displayDataInternal.userSkills.skillsLevel" :total-num-levels="displayDataInternal.userSkills.totalLevels"/>
                </div>

                <div class="text-center col-md-4">
                    <circle-progress
                            :total-completed-points="displayDataInternal.userSkills.levelPoints"
                            :points-completed-today="displayDataInternal.userSkills.todaysPoints"
                            :total-possible-points="displayDataInternal.userSkills.levelTotalPoints"
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
                                    <strong>{{ displayDataInternal.userSkills.todaysPoints | number}}</strong> Points earned Today
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
                <point-progress-chart ref="pointProgressChart" />
            </div>

            <div v-if="hasBadges" class="col-lg-3">
                <my-badges :num-badges-completed="displayDataInternal.userSkills.badges.numBadgesCompleted"></my-badges>
            </div>
        </div>
    </div>
</template>

<script>
  import PointProgressChart from '@/userSkills/pointProgress/PointProgressChart';

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
    data() {
      return {
        displayDataInternal: this.displayData,
      };
    },
    watch: {
      displayData: {
        deep: true,
        handler() {
          this.displayDataInternal = this.displayData;
          this.$refs.pointProgressChart.loadPointsHistory();
        },
      },
    },
    computed: {
      isLevelComplete() {
        return this.displayDataInternal.userSkills.levelTotalPoints === -1;
      },
      hasBadges() {
        return this.displayDataInternal.userSkills && this.displayDataInternal.userSkills.badges && this.displayDataInternal.userSkills.badges.enabled;
      },
      levelStats() {
        return {
          title: this.isLevelComplete ? 'Level Progress' : `Level ${this.displayDataInternal.userSkills.skillsLevel + 1} Progress`,
          nextLevel: this.displayDataInternal.userSkills.skillsLevel + 1,
          pointsTillNextLevel: this.displayDataInternal.userSkills.levelTotalPoints - this.displayDataInternal.userSkills.levelPoints,
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
    methods: {
      refreshData(displayData) {
        if (displayData) {
          this.displayDataInternal = displayData;
        }
      },
    },
  };
</script>

<style scoped>

</style>
