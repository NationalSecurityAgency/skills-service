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
  <div class="row pt-2">
    <div v-if="skill.description" v-show="skill.description.href" class="col text-left">
      <div class="btn-group" role="group" aria-label="Basic example">
        <a :href="skill.description.href" target="_blank" rel="noopener" class="btn btn-outline-info ">
          <i class="fas fa-question-circle"></i> Learn More <i class="fas fa-external-link-alt"></i>
        </a>
        <button v-if="selfReport.available" class="btn btn-outline-info"
                :disabled="selfReportDisabled"
                @click="reportSkill"
                data-cy="selfReportBtn">
          <i class="fas fa-check-square"></i> I did it
        </button>
      </div>
    </div>
    <div class="col-12">
      <div v-if="isPointsEarned && !selfReport.msgHidden" class="alert alert-success mt-2" role="alert" data-cy="selfReportAlert">
        <i class="far fa-thumbs-up"></i> Congrats! You just earned <span class="text-success font-weight-bold">{{ selfReport.res.pointsEarned }}</span> points!
        <button type="button" class="close" data-dismiss="alert" aria-label="Close" @click="selfReport.msgHidden = true">
          <span aria-hidden="true">&times;</span>
        </button>
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
      selfReportDisabled() {
        return this.isCompleted();
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
          });
      },
    },
  };
</script>

<style scoped>

</style>
