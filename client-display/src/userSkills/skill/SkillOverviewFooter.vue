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
    <div v-if="isQuizOrSurveySkill && selfReport.available" class="mb-2 alert alert-info">
      <div class="row">
        <div class="col font-italic pt-1" data-cy="quizAlert">
          <i class="fas fa-user-check font-size-2" aria-hidden="true"></i>
          {{ isSurveySkill ? 'Complete' : 'Pass'}} the<span
            v-if="skillInternal.selfReporting.numQuizQuestions && skillInternal.selfReporting.numQuizQuestions > 0">&nbsp;{{
            skillInternal.selfReporting.numQuizQuestions
          }}-question</span>&nbsp;<b>{{ skillInternal.selfReporting.quizName }}</b>&nbsp;{{ isSurveySkill ? 'Survey' : 'Quiz'}} and earn <span class="font-size-1"><b-badge
            variant="info">{{ skillInternal.totalPoints | number }}</b-badge></span> points!
        </div>
        <div class="col-auto text-right">
          <b-button v-if="selfReport.available && isQuizOrSurveySkill"
                    class="skills-theme-btn"
                    :disabled="selfReportDisabled"
                    variant="info"
                    @click="navToQuiz"
                    data-cy="takeQuizBtn">
            <span v-if="isQuizSkill">Take Quiz</span>
            <span v-if="isSurveySkill">Complete Survey</span>
            <i class="far fa-arrow-alt-circle-right ml-1"></i>
          </b-button>
        </div>
      </div>
    </div>
    <div v-if="isHonorSystem && selfReport.available" class="mb-2 alert alert-info">
      <div class="row">
        <div class="col font-italic pt-1" data-cy="honorSystemAlert">
          <i class="fas fa-user-shield font-size-2" aria-hidden="true"></i>
          This skill can be submitted under the <span class="font-size-1">Honor System</span>, claim <span class="font-size-1"><b-badge variant="info">{{ skillInternal.pointIncrement | number }}</b-badge></span> points once you've completed the skill.
        </div>
        <div class="col-auto">
          <b-button class="skills-theme-btn"
                    :disabled="selfReportDisabled"
                    variant="info"
                    @click="reportSkill(null)"
                    data-cy="claimPointsBtn">
            Claim Points
            <i class="fas fa-check-double ml-1" aria-hidden="true"></i>
          </b-button>
        </div>
      </div>
    </div>
    <div v-if="isApprovalRequired && selfReport.available && !selfReportDisabled && !isRejected" class="mb-2 alert alert-info">
      <div class="row">
        <div class="col font-italic pt-1" data-cy="requestApprovalAlert">
          <i class="fas fa-traffic-light font-size-2" aria-hidden="true"></i>
          This skill requires <span class="font-size-1">approval</span>.
          Request <span class="font-size-1"><b-badge variant="info">{{ skillInternal.pointIncrement | number }}</b-badge></span> points once you've completed the skill.
        </div>
        <div class="col-auto">
          <b-button v-if="!showApprovalJustification"
                    ref="beginRequestBtn"
                    class="skills-theme-btn"
                    :disabled="selfReportDisabled"
                    variant="info"
                    @click="showApprovalJustification = true"
                    data-cy="requestApprovalBtn">
            Begin Request
            <i class="far fa-arrow-alt-circle-right ml-1" aria-hidden="true"></i>
          </b-button>
        </div>
      </div>
      <b-overlay :show="requestApprovalLoading">
        <justification-input v-if="showApprovalJustification"
                           class="mt-1"
                           @report-skill="reportSkill"
                           @cancel="showApprovalJustification = false; focusOnRef('beginRequestBtn')"
                           :skill="skillInternal"
                           :is-approval-required="isApprovalRequired"
                           :is-honor-system="isHonorSystem"
                           :is-justitification-required="isJustificationRequired" />
      </b-overlay>
    </div>
    <div v-if="isPendingApproval() && selfReport.msgHidden" class="mb-2 alert alert-info font-italic" data-cy="pendingApprovalStatus">
      <i class="far fa-clock font-size-2" aria-hidden="true" /> This skill is <span class="font-size-1 normal-font">pending approval</span>.
        Submitted <span class="text-info">{{ skillInternal.selfReporting.requestedOn | relativeTime}}</span>.
    </div>
    <div v-if="isRejected" class="alert alert-danger mt-2"  data-cy="selfReportRejectedAlert">
      <b-overlay :show="removeRejectionLoading"
                 spinner-type="grow"
                 spinner-small
                 variant="transparent">
        <div class="row">
          <div class="col">
            <i class="fas fa-heart-broken font-size-2" aria-hidden=""></i>
            Unfortunately your request from <b>{{ skillInternal.selfReporting.requestedOn | formatDate('MM/DD/YYYY') }}</b> was rejected <span class="text-info">{{ skillInternal.selfReporting.rejectedOn | relativeTime}}</span>. The reason is:
            <b>"{{ skillInternal.selfReporting.rejectionMsg }}"</b>
          </div>
          <div class="col-auto text-right">
            <button class="btn btn-info" data-cy="clearRejectionMsgBtn" @click="removeRejection">
              <i class="fas fa-check" aria-hidden="true"></i> I got it!
            </button>
          </div>
        </div>
      </b-overlay>
    </div>

    <div v-if="errNotification.enable" class="alert alert-danger mt-2" role="alert" data-cy="selfReportError">
      <i class="fas fa-exclamation-triangle" /> {{ errNotification.msg }}
    </div>
    <div v-if="!selfReport.msgHidden" class="alert alert-success mt-2" role="alert" data-cy="selfReportAlert">
      <div class="row">
        <div class="col">
          <div v-if="isPointsEarned">
            <i class="fas fa-birthday-cake text-success mr-2" style="font-size: 1.5rem"></i> Congrats! You just earned <span
              class="text-success font-weight-bold">{{ selfReport.res.pointsEarned }}</span> points<span v-if="isCompleted"> and <b>completed</b> the {{ skillDisplayName.toLowerCase() }}</span>!
          </div>
          <div v-if="!isPointsEarned && (this.isAlreadyPerformed() || !isApprovalRequired)">
            <i class="fas fa-cloud-sun-rain mr-2 text-info" style="font-size: 1.5rem"></i> <span> <b class="text-info">Unfortunately</b> no points.</span>
            {{ this.selfReport.res.explanation }}
          </div>

          <div v-if="!this.isAlreadyPerformed() && isApprovalRequired">
            <div>
              <i class="fas fa-user-clock mr-2 text-info" style="font-size: 1.5rem"></i> <b>Submitted successfully!</b>
            </div>
            <div class="mt-1">
              This {{ skillDisplayName.toLowerCase() }} <b class="text-info">requires approval</b> from a {{ projectDisplayName.toLowerCase() }} administrator. Now let's play the waiting game!
            </div>
          </div>
        </div>
        <div class="col-auto">
          <button type="button" class="close" data-dismiss="alert" aria-label="Close"
                  @click="selfReport.msgHidden = true" data-cy="dismissSuccessfulSubmissionBtn">
            <i class="fas fa-times-circle" aria-hidden="true"></i>
          </button>
        </div>
      </div>
    </div>

    <div class="row pt-2">
      <div class="col-auto text-left">
        <div class="btn-group" role="group" aria-label="Skills Buttons">
          <a v-if="skillInternal.description && skillInternal.description.href" :href="skillInternal.description.href" target="_blank" rel="noopener" class="btn btn-outline-info skills-theme-btn">
            <i class="fas fa-question-circle"></i> Learn More <i class="fas fa-external-link-alt"></i>
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import NavigationErrorMixin from '@/common/utilities/NavigationErrorMixin';
  import UserSkillsService from '../service/UserSkillsService';
  import JustificationInput from './JustificationInput';

  export default {
    name: 'SkillOverviewFooter',
    mixins: [NavigationErrorMixin],
    components: { JustificationInput },
    props: ['skill'],
    data() {
      return {
        skillInternal: {},
        showApprovalJustification: false,
        requestApprovalLoading: false,
        removeRejectionLoading: false,
        rejectionDialogYOffset: 0,
        approvalRequestedMsg: '',
        selfReport: {
          available: true,
          res: null,
          msgHidden: true,
        },
        errNotification: {
          enable: false,
          msg: '',
        },
      };
    },
    mounted() {
      this.skillInternal = { ...this.skill };
      this.selfReport.available = this.selfReportConfigured() && !this.isCompleted && !this.isLocked() && !this.isCrossProject();
    },
    computed: {
      isPointsEarned() {
        return this.selfReport && this.selfReport.res && this.selfReport.res.skillApplied;
      },
      isCompleted() {
        return this.skillInternal.points === this.skillInternal.totalPoints;
      },
      selfReportDisabled() {
        return this.isCompleted || this.isPendingApproval();
      },
      isHonorSystem() {
        return this.skillInternal.selfReporting && this.skillInternal.selfReporting.type === 'HonorSystem';
      },
      isApprovalRequired() {
        return this.skillInternal.selfReporting && this.skillInternal.selfReporting.type === 'Approval';
      },
      isJustificationRequired() {
        return this.skillInternal.selfReporting && this.skillInternal.selfReporting.justificationRequired;
      },
      isRejected() {
        const res = this.skillInternal.selfReporting && this.skillInternal.selfReporting.rejectedOn !== null && this.skillInternal.selfReporting.rejectedOn !== undefined;
        return res;
      },
      isQuizSkill() {
        return this.skillInternal && this.skillInternal.selfReporting && this.skillInternal.selfReporting.type === 'Quiz';
      },
      isSurveySkill() {
        return this.skillInternal && this.skillInternal.selfReporting && this.skillInternal.selfReporting.type === 'Survey';
      },
      isQuizOrSurveySkill() {
        return this.isQuizSkill || this.isSurveySkill;
      },
    },
    methods: {
      navToQuiz() {
        this.handlePush({
          name: 'quizPage',
          params: {
            skillInternal: this.skillInternal.subjectId, skillId: this.skillInternal.skillId, quizId: this.skillInternal.selfReporting.quizId, skill: this.skillInternal,
          },
        });
      },
      isPendingApproval() {
        const res = this.skillInternal.selfReporting && this.skillInternal.selfReporting.requestedOn !== null && this.skillInternal.selfReporting.requestedOn !== undefined && !this.isRejected;
        return res;
      },
      selfReportConfigured() {
        return this.skillInternal.selfReporting && this.skillInternal.selfReporting && this.skillInternal.selfReporting.enabled;
      },
      isLocked() {
        return this.skillInternal.dependencyInfo && !this.skillInternal.dependencyInfo.achieved;
      },
      isCrossProject() {
        return this.skillInternal.crossProject;
      },
      isAlreadyPerformed() {
        return this.selfReport.res && this.selfReport.res.explanation.includes('was already performed');
      },
      removeRejection() {
        this.removeRejectionLoading = true;
        UserSkillsService.removeApprovalRejection(this.skillInternal.selfReporting.approvalId)
          .then(() => {
            this.skillInternal.selfReporting.rejectedOn = null;
            this.skillInternal.selfReporting.rejectedMsg = null;
            this.skillInternal.selfReporting.requestedOn = null;
          }).finally(() => {
            this.removeRejectionLoading = false;
          });
      },
      reportSkill(approvalRequestedMsg) {
        this.errNotification.enable = false;
        this.errNotification.msg = '';

        this.requestApprovalLoading = true;
        UserSkillsService.reportSkill(this.skillInternal.skillId, approvalRequestedMsg)
          .then((res) => {
            if (res.explanation.includes('This skill was already submitted for approval and is still pending approval')
              || res.explanation.includes('This skill reached its maximum points')) {
              this.errNotification.msg = `${res.explanation}. Please refresh the page to update the status.`;
              this.errNotification.enable = true;
            } else {
              if (this.skillInternal.selfReporting) {
                this.skillInternal.selfReporting.rejectedOn = null;
                this.skillInternal.selfReporting.rejectionMsg = null;
              }

              this.selfReport.msgHidden = false;
              this.selfReport.res = res;
              if (!this.isAlreadyPerformed() && this.isApprovalRequired) {
                this.skillInternal.selfReporting.requestedOn = new Date();
              }
              this.updateEarnedPoints(res);
            }
          }).catch((e) => {
            if (e.response.data && e.response.data.errorCode
              && (e.response.data.errorCode === 'InsufficientProjectPoints' || e.response.data.errorCode === 'InsufficientSubjectPoints')) {
              this.errNotification.msg = e.response.data.explanation;
              this.errNotification.enable = true;
            } else {
              const errorMessage = (e.response && e.response.data && e.response.data.explanation) ? e.response.data.explanation : undefined;
              this.$router.push({
                name: 'error',
                params: {
                  errorMessage,
                },
              });
            }
          }).finally(() => {
            this.requestApprovalLoading = false;
          });
      },
      testWasTaken(testResult) {
        const { gradedRes } = testResult;
        if (gradedRes && gradedRes.passed && gradedRes.associatedSkillResults) {
          const skill = gradedRes.associatedSkillResults.find((e) => e.projectId === this.skillInternal.projectId && e.skillId === this.skillInternal.skillId);
          this.updateEarnedPoints(skill);
        }
      },
      updateEarnedPoints(res) {
        if (res.pointsEarned > 0) {
          this.skillInternal.points += res.pointsEarned;
          this.$emit('points-earned', res.pointsEarned);
        }
      },
      focusOnRef(ref) {
        this.$nextTick(() => {
          this.$refs[ref]?.focus();
        });
      },
    },
  };
</script>

<style scoped>
.font-size-1 {
  font-size: 1rem
}
.font-size-2 {
  font-size: 1.2rem
}
.normal-font {
  font-style: normal;
}
</style>
