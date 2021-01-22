<template>
  <div class="row pt-2">
    <div v-if="skill.description" v-show="skill.description.href" class="col text-left">
      <div class="btn-group" role="group" aria-label="Basic example">
        <a :href="skill.description.href" target="_blank" rel="noopener" class="btn btn-outline-info ">
          <i class="fas fa-question-circle"></i> Learn More <i class="fas fa-external-link-alt"></i>
        </a>
        <button v-if="selfReport.available" class="btn btn-outline-info" @click="reportSkill"><i class="fas fa-check-square"></i> I did it</button>
      </div>
    </div>
    <div class="col-12">
      <div v-if="isPointsEarned && !selfReport.msgHidden" class="alert alert-success mt-2" role="alert">
        <i class="far fa-thumbs-up"></i> Congrats! You just earned <span class="text-success font-weight-bold">{{ selfReport.res.pointsEarned }}</span> points!
      </div>
    </div>
  </div>
</template>

<script>
  import UserSkillsService from '../service/UserSkillsService';

  export default {
    name: 'SkillOverviewFooter',
    props: ['skill'],
    data() {
      return {
        selfReport: {
          available: true,
          res: null,
          msgHidden: true,
        },
      };
    },
    mounted() {
      this.selfReport.available = !this.isCompleted() && !this.isLocked() && !this.isCrossProject();
    },
    computed: {
      isPointsEarned() {
        return this.selfReport && this.selfReport.res && this.selfReport.res.skillApplied;
      },
    },
    methods: {
      isCompleted() {
        return this.skill.points === this.skill.totalPoints;
      },
      isLocked() {
        return this.skill.dependencyInfo && !this.skill.dependencyInfo.achieved;
      },
      isCrossProject() {
        return this.skill.crossProject;
      },
      reportSkill() {
        UserSkillsService.reportSkill(this.skill.skillId)
          .then((res) => {
            this.selfReport.msgHidden = false;
            this.selfReport.res = res;
            this.$emit('points-earned', res.pointsEarned);
            this.hideMsgAfterTimeout();
          });
      },
      hideMsgAfterTimeout() {
        setTimeout(() => {
          this.selfReport.msgHidden = true;
          if (this.skill.points === this.skill.totalPoints) {
            this.selfReport.available = false;
          }
        }, 500000);
      },
    },
  };
</script>

<style scoped>

</style>
