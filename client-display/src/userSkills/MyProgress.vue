<template>
  <div class="progress-circle-wrapper">
    <label class="skill-tile-label">{{ progressLabel }}</label>
    <div class="progress-circle">
      <div class="circle-number">
        <span v-if="isLevelComplete">
          <i class="fa fa-check fa-2x item-complete-icon"/>
        </span>
        <span v-if="!isLevelComplete">
          {{ userSkills.levelPoints | number }}
        </span>
      </div>
      <circle-progress
        v-if="isLevelComplete"
        :total-completed-points="100"
        :points-completed-today="100"
        :total-possible-points="100"
        :incompleteColor="'transparent'"
        totalCompletedColor="#59ad52"/>
      <popper
        trigger="hover"
        :append-to-body="true">
        <div
          slot="reference">
          <circle-progress
            v-if="!isLevelComplete"
            :total-completed-points="userSkills.levelPoints"
            :points-completed-today="userSkills.todaysPoints"
            :total-possible-points="userSkills.levelTotalPoints" />
        </div>
        <div class="popper">
          <user-skills-progress-bar-summary :template-data="levelPopupObj"/>
        </div>
      </popper>
    </div>
    <p v-if="isLevelComplete">All levels complete</p>
    <p v-if="!isLevelComplete">{{ ( userSkills.levelTotalPoints - userSkills.levelPoints ) | number }} points to
      next level</p>
  </div>
</template>

<script>
  import CircleProgress from '@/common/progress/CircleProgress.vue';
  import MyProgressSummary from '@/userSkills/MyProgressSummary.vue';

  import Popper from 'vue-popperjs';

  export default {
    components: {
      CircleProgress,
      MyProgressSummary,
      Popper
    },
    props: {
      mode: {
        userSkills: {
          type: Object,
          required: true,
        },
        type: String,
        required: true,
        validate(value) {
          return ['progress', 'points'].includes(value);
        },
      },
    },
    data() {
      return {
        isLevelComplete: false,
        levelPopupObj: null,
        subjectTotalPopupObj: null,
        totalPopupObj: null,
      };
    },
    computed: {
      progressLabel() {
        return this.type === 'progress' ? 'My Progress' : 'My Points';
      },
    },
    mounted() {
      if (this.userSkills.levelTotalPoints === -1) {
        this.isLevelComplete = true;
      }

      this.levelPopupObj = {
        title: 'Points Earned Toward Next Level',
        overallPoints: this.userSkills.levelPoints,
        todaysPoints: this.userSkills.todaysPoints < this.userSkills.levelPoints ? this.userSkills.todaysPoints : this.userSkills.levelPoints,
      };

      this.subjectTotalPopupObj = {
        title: 'Points Earned for this Subject',
        overallPoints: this.userSkills.points,
        todaysPoints: this.userSkills.todaysPoints,
      };

      this.totalPopupObj = {
        title: 'Total Points Earned',
        overallPoints: this.userSkills.points,
        todaysPoints: this.userSkills.todaysPoints,
      };
    },
  }
</script>

<style scoped>

</style>
