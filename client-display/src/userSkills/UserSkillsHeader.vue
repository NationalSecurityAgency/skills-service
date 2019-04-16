<template>
  <div class="user-skills-overview">
    <div class=" card">
      <div class="row card-body">
        <div class="text-center col-md-4">
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

        <div class="text-center col-md-4">
          <my-skill-level :skill-level="userSkills.skillsLevel" />
        </div>

        <div class="text-center col-md-4">
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
    </div>

    <div class="row pt-3">
      <div class="col-lg-3">
        <my-rank
          v-if="userSkillsRanking"
          :rank="userSkillsRanking.position"
          :subject="userSkills.subject" />
      </div>

      <div
        id="point-progress-container"
        :class="{ 'col-lg-6' : hasBadges, 'col-lg-9' : !hasBadges }">
        <point-progress-chart
          :points-history="pointsHistory"/>
      </div>

      <div v-if="hasBadges" class="col-lg-3">
        <my-badges :num-badges-completed="userSkills.badges.numBadgesCompleted"></my-badges>
      </div>
    </div>
  </div>
</template>

<script>
  import MyProgressSummary from '@/userSkills/MyProgressSummary.vue';
  import PointProgressChart from '@/userSkills/PointProgressChart.vue';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';

  import CircleProgress from '@/common/progress/CircleProgress.vue';
  import Popper from 'vue-popperjs';
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
      Popper,
      MyRank,
      MySkillLevel,
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
    computed: {
      hasBadges() {
        return this.userSkills && this.userSkills.badges && this.userSkills.badges.enabled;
      },
    },
  };
</script>

<style scoped>

</style>
