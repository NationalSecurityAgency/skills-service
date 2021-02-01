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
  <div>
    <div class="row pt-2">
    <div class="col text-left">
      <div class="btn-group" role="group" aria-label="Basic example">
        <a v-if="skill.description && skill.description.href" :href="skill.description.href" target="_blank" rel="noopener" class="btn btn-outline-info ">
          <i class="fas fa-question-circle"></i> Learn More <i class="fas fa-external-link-alt"></i>
        </a>
        <button v-if="selfReport.available" class="btn btn-outline-info"
                :disabled="selfReportDisabled"
                @click="modalVisible = true;"
                data-cy="selfReportBtn">
          <i class="fas fa-check-square"></i> I did it
        </button>
      </div>
    </div>
    <div class="col-12">
      <div v-if="!selfReport.msgHidden" class="alert alert-success mt-2" role="alert" data-cy="selfReportAlert">
        <div v-if="isPointsEarned">
          <i class="far fa-thumbs-up"></i> Congrats! You just earned <span class="text-success font-weight-bold">{{ selfReport.res.pointsEarned }}</span> points!
          <button type="button" class="close" data-dismiss="alert" aria-label="Close" @click="selfReport.msgHidden = true">
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
        <div v-if="selfReportConfigured && isApprovalRequired">
          This skills requires project administrator approval. Submitted successfully! Now let's play the waiting game!
        </div>
      </div>
    </div>
  </div>

    <b-modal id="reportSkillModal"
             title="REPORT SKILL"
             ok-title="Submit"
             :no-close-on-backdrop="true"
             v-model="modalVisible">
      <div class="row p-2">
        <div class="col-auto text-center">
          <i v-if="isHonorSystem" class="fas fa-chess-knight text-success" style="font-size: 3rem"></i>
          <i v-if="isApprovalRequired" class="fas fa-thumbs-up text-info" style="font-size: 3rem"></i>
        </div>
        <div class="col">
          <p class="h5" v-if="isHonorSystem">This skill can submitted under the <b class="text-success">Honor System</b> and <b class="text-success">{{ skill.pointIncrement}}</b> points will apply right away!</p>
          <p class="h5" v-if="isApprovalRequired">This skill requires <b class="text-info">approval</b>. Submit with an <span class="text-muted">optional</span> message and it will enter an approval queue.</p>
        </div>
      </div>
      <input type="text" id="approvalRequiredMsg"
             v-if="isApprovalRequired" v-model="approvalRequestedMsg"
             class="form-control" placeholder="Message (optional)">
      <template #modal-footer>
        <button type="button" class="btn btn-outline-danger text-uppercase" @click="modalVisible=false">
          <i class="fas fa-times-circle"></i> Cancel
        </button>
        <button type="button" class="btn btn-outline-success text-uppercase" @click="reportSkill(); modalVisible=false;">
          <i class="fas fa-arrow-alt-circle-right"></i> Submit
        </button>
      </template>
    </b-modal>
  </div>
</template>

<script>
  import UserSkillsService from '../service/UserSkillsService';

  export default {
    name: 'SkillOverviewFooter',
    props: ['skill'],
    data() {
      return {
        modalVisible: false,
        approvalRequestedMsg: '',
        selfReport: {
          available: true,
          res: null,
          msgHidden: true,
        },
      };
    },
    mounted() {
      this.selfReport.available = this.selfReportConfigured() && !this.isCompleted() && !this.isLocked() && !this.isCrossProject();
    },
    computed: {
      isPointsEarned() {
        return this.selfReport && this.selfReport.res && this.selfReport.res.skillApplied;
      },
      selfReportDisabled() {
        return this.isCompleted();
      },
      isHonorSystem() {
        return this.skill.selfReportingType === 'HonorSystem';
      },
      isApprovalRequired() {
        return this.skill.selfReportingType === 'Approval';
      },
    },
    methods: {
      selfReportConfigured() {
        return this.skill.selfReportingType && this.skill.selfReportingType.length > 0;
      },
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
        UserSkillsService.reportSkill(this.skill.skillId, this.approvalRequestedMsg)
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
