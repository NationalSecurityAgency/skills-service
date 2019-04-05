<template>
  <div class="user-skills-overview">
    <div
      class="stats-container"
      style="margin: 0 75px; overflow: hidden;">
      <div class="text-center col-xs-12 col-md-4">
        <circle-progress
          :user-skills="userSkills"
          :total-completed-points="userSkills.levelPoints"
          :points-completed-today="userSkills.todaysPoints"
          :total-possible-points="userSkills.levelTotalPoints"
          :total-completed-color="isLevelComplete ? '#59ad52' : '#7ed6f3'"
          title="My Progress">
          <div slot="footer">
            <p v-if="isLevelComplete">All levels complete</p>
            <p v-if="!isLevelComplete">{{ ( userSkills.levelTotalPoints - userSkills.levelPoints ) | number }} points to next level</p>
          </div>
        </circle-progress>
      </div>

      <div class="text-center col-xs-12 col-md-4">
        <my-skill-level :skill-level="userSkills.skillsLevel" />
      </div>

      <div class="text-center col-xs-12 col-md-4">
        <circle-progress
          :user-skills="userSkills"
          :total-completed-points="userSkills.points"
          :points-completed-today="userSkills.todaysPoints"
          :total-possible-points="userSkills.totalPoints"
          :total-completed-color="userSkills.points === userSkills.totalPoints ? '#59ad52' : '#7ed6f3'"
          title="My Points">
          <div slot="footer">
            <p v-if="userSkills.points === userSkills.totalPoints">Total points earned</p>
            <p v-if="userSkills.points !== userSkills.totalPoints">{{ userSkills.totalPoints | number }} total points</p>
          </div>
        </circle-progress>
      </div>
    </div>

    <hr>

    <div class="row">
      <div class="col-lg-3">
        <my-rank
          v-if="userSkillsRanking"
          :rank="userSkillsRanking.position"
          :subject="userSkills.subject" />
      </div>

      <div
        id="point-progress-container"
        class="col-lg-9">
        <point-progress-chart
          v-if="pointsHistory && pointsHistory.length > 0"
          :points-history="pointsHistory"/>
        <not-enough-data-chart-placeholder v-if="!pointsHistory || pointsHistory.length === 0" />
      </div>
    </div>
  </div>
</template>

<script>
  import MyProgressSummary from '@/userSkills/MyProgressSummary.vue';
  import PointProgressChart from '@/userSkills/PointProgressChart.vue';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import NotEnoughDataChartPlaceholder from '@/userSkills/NotEnoughDataChartPlaceholder.vue';

  import CircleProgress from '@/common/progress/CircleProgress.vue';
  import Popper from 'vue-popperjs';
  import StarProgress from '@/common/progress/StarProgress.vue';
  import MyRank from '@/userSkills/myRank/MyRank.vue';
  import MySkillLevel from '@/userSkills/MySkillLevel.vue';

  export default {
    components: {
      PointProgressChart,
      MyProgressSummary,
      CircleProgress,
      StarProgress,
      Popper,
      MyRank,
      MySkillLevel,
      NotEnoughDataChartPlaceholder,
    },
    props: {
      userSkills: Object,
    },
    data() {
      return {
        isLevelComplete: false,
        maxLevel: 5,
        isLoaded: true,
        userSkillsRanking: null,
        pointsHistory: null,
      };
    },
    watch: {
      userSkills(newSkills) {
        this.setUserSkillsRanking(newSkills.subjectId);
        this.setPointsHistory(newSkills.subjectId);
      },
    },
    mounted() {
      if (this.userSkills.levelTotalPoints === -1) {
        this.isLevelComplete = true;
      }

      this.setUserSkillsRanking(this.userSkills.subjectId);
      this.setPointsHistory(this.userSkills.subjectId);
    },
    methods: {
      setUserSkillsRanking(subjectId) {
        UserSkillsService.getUserSkillsRanking(subjectId)
          .then((response) => {
            this.userSkillsRanking = response;
          });
      },

      setPointsHistory(subjectId) {
        UserSkillsService.getPointsHistory(subjectId)
          .then((result) => {
            this.pointsHistory = result;
          });
      },
    },
  };
</script>

<style scoped src="../assets/bootstrap4tmp/bootstrap.min.css">
</style>

<style>
  .stats-container {
    padding-bottom: 20px;
    overflow: auto;
    text-align: center;
  }

  .progress {
    background-color: #eaeaea;
    border-radius: 0;
    margin-bottom: 0;
    position: relative;
  }

  .progress .progress-bar {
    line-height: 20px;
    overflow: hidden;
    position: absolute;
  }

  .progress-loading {
    width: 100%;
    transition: opacity 0.6s ease 0s;
    position: absolute;
  }

  .progress-loading-fixed{
    z-index: 300;
    position: fixed;
    top: 0;
  }

  .progress-loading .empty-bar, .progress-loading .full-bar{
    opacity: 0;
  }

  .full-bar > .progress-bar {
    background-color: #59ad52;
  }

  .progress-xxs {
    height: 2px;
  }

  .progress-xs {
    height: 6px;
  }

  .progress-sm {
    height: 12px;
  }

  .progress-sm .progress-bar {
    font-size: 10px;
    line-height: 1em;
  }

  .progress,
  .progress-bar {
    -webkit-box-shadow: none;
    box-shadow: none;
  }

  .progress-bar-primary {
    background-color: #7266ba;
  }

  .progress-bar-info {
    background-color: #23b7e5;
  }

  .progress-bar-success {
    background-color: #27c24c;
  }

  .progress-bar-warning {
    background-color: #fad733;
  }

  .progress-bar-danger {
    background-color: #f05050;
  }

  .progress-bar-black {
    background-color: #1c2b36;
  }

  .progress-bar-white {
    background-color: #fff;
  }

  .progress.right .progress-bar {
    right: 0;
  }
  .progress.vertical {
    float: left;
    height: 100%;
    margin-right: 20px;
    width: 20px;
  }
  .progress.vertical.bottom {
    position: relative;
  }
  .progress.vertical .progress-bar {
    height: 0;
    transition: height 0.6s ease 0s;
    width: 100%;
  }
  .progress.vertical.bottom .progress-bar {
    bottom: 0;
    position: absolute;
  }
</style>
