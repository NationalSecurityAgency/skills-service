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
      <div class="col-auto text-left">
        <div class="btn-group" role="group" aria-label="Basic example">
          <a v-if="skillInternal.description && skillInternal.description.href" :href="skillInternal.description.href" target="_blank" rel="noopener" class="btn btn-outline-info skills-theme-btn">
            <i class="fas fa-question-circle"></i> Learn More <i class="fas fa-external-link-alt"></i>
          </a>
          <button v-if="selfReport.available && !isQuizOrSurveySkill" class="btn btn-outline-info skills-theme-btn"
                  :disabled="selfReportDisabled"
                  @click="showSelfReportModal"
                  data-cy="selfReportBtn">
            <i class="fas fa-check-square mr-1"></i>
            I did it
          </button>
          <b-button v-if="selfReport.available && isQuizOrSurveySkill"
                    class="skills-theme-btn"
                    :disabled="selfReportDisabled"
                    variant="outline-info"
                    @click="navToQuiz"
                    data-cy="takeQuizBtn">
            <i class="fas fa-check-square mr-1"></i>
            <span v-if="isQuizSkill">Take Quiz</span>
            <span v-if="isSurveySkill">Complete Survey</span>
          </b-button>
        </div>
      </div>
      <div v-if="isQuizOrSurveySkill" class="col text-right">
        <div class="font-italic text-secondary">
          <i class="fas fa-user-edit text-info mr-1" style="font-size: 1.2rem;"></i>
          <span v-if="isQuizSkill">Quiz</span><span v-if="isSurveySkill">Survey</span> Completion<span v-if="!selfReportDisabled"> Required</span>:
        </div>
        <div>
          <span v-if="selfReportDisabled"
              class="font-weight-bold text-primary"  style="font-size: 1rem;">{{ skillInternal.selfReporting.quizName }}</span>
          <b-button v-else variant="link"
                    tag="a"
                    class="p-0"
                    @click="navToQuiz"
                    data-cy="quizLink">
            <span class="font-weight-bold text-primary" style="font-size: 1rem;">{{
                skillInternal.selfReporting.quizName
              }}</span>
          </b-button>
        </div>
      </div>
      <div v-if="isPendingApproval()" class="col text-right" data-cy="pendingApprovalStatus">
        <div>
          <i class="far fa-clock"></i> Pending Approval
        </div>
        <div>
          Submitted <span class="text-info">{{ skillInternal.selfReporting.requestedOn | relativeTime}}</span>
        </div>
      </div>
      <div v-if="isRejected" class="col text-right"  data-cy="approvalRejectedStatus">
        <div class="text-danger">
          <i class="fas fa-ban"></i> Approval Rejected
        </div>
        <div>
          Rejected <span class="text-info">{{ skillInternal.selfReporting.rejectedOn | relativeTime}}</span>
        </div>
      </div>
    </div>
    <div v-if="isRejected" class="alert alert-danger mt-2"  data-cy="selfReportRejectedAlert">
      <div class="row">
        <div class="col-auto">
          <i class="fas fa-heart-broken" style="font-size: 1.5rem;"></i>
        </div>
        <div class="col">
          <div>
            Unfortunately your request from <b>{{ skillInternal.selfReporting.requestedOn | formatDate('MM/DD/YYYY') }}</b> was rejected. The reason is:
            <b>"{{ skillInternal.selfReporting.rejectionMsg }}"</b>
          </div>
        </div>
        <div class="col-auto text-right">
          <button class="btn btn-info" data-cy="clearRejectionMsgBtn" @click="showRejectionModal">
            <i class="fas fa-check"></i> I got it!
          </button>
        </div>
      </div>
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
                  @click="selfReport.msgHidden = true">
            <i class="fas fa-times-circle"></i>
          </button>
        </div>
      </div>
    </div>

    <self-report-skill-modal v-if="selfReportModalVisible" @report-skill="reportSkill" @cancel="selfReportModalVisible = false"
        ref="selfReportModal"
        :skill="skillInternal"
        :is-approval-required="isApprovalRequired"
        :is-honor-system="isHonorSystem"
        :is-justitification-required="isJustificationRequired" />

    <b-modal id="clearRejectionMsgDialog"
             title="CLEAR APPROVAL REJECTION"
             :no-close-on-backdrop="true"
             v-model="clearRejectionModalVisible">
      <modal-positioner :y-offset="rejectionDialogYOffset" />
      <div class="row p-2" data-cy="clearRejectionMsgDialog">
        <div class="col-auto text-center">
          <i class="fas fa-exclamation-triangle text-danger" style="font-size: 3rem"></i>
        </div>
        <div class="col">
          This action will <b class="text-danger">permanently</b> remove the rejection and its message. <span class="text-danger">Are you sure</span>?
        </div>
      </div>
      <template #modal-footer>
        <button type="button" class="btn btn-outline-danger text-uppercase" @click="clearRejectionModalVisible=false">
          <i class="fas fa-times-circle"></i> Cancel
        </button>
        <button type="button" class="btn btn-outline-success text-uppercase" @click="removeRejection(); clearRejectionModalVisible=false;" data-cy="removeRejectionBtn">
          <i class="fas fa-arrow-alt-circle-right"></i> Remove
        </button>
      </template>
    </b-modal>
  </div>
</template>

<script>
  import NavigationErrorMixin from '@/common/utilities/NavigationErrorMixin';
  import UserSkillsService from '../service/UserSkillsService';
  import SelfReportSkillModal from './SelfReportSkillModal';
  import ModalPositioner from './ModalPositioner';

  export default {
    name: 'SkillOverviewFooter',
    mixins: [NavigationErrorMixin],
    components: { ModalPositioner, SelfReportSkillModal },
    props: ['skill'],
    data() {
      return {
        skillInternal: {},
        selfReportModalVisible: false,
        clearRejectionModalVisible: false,
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
      showSelfReportModal(event) {
        this.selfReportModalVisible = true;
        this.$nextTick(() => {
          this.$refs.selfReportModal.updatePosition(event.pageY);
        });
      },
      navToQuiz() {
        this.handlePush({
          name: 'quizPage',
          params: {
            skillInternal: this.skillInternal.subjectId, skillId: this.skillInternal.skillId, quizId: this.skillInternal.selfReporting.quizId, skill: this.skillInternal,
          },
        });
      },
      showRejectionModal(event) {
        this.clearRejectionModalVisible = true;
        this.rejectionDialogYOffset = event.pageY;
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
        UserSkillsService.removeApprovalRejection(this.skillInternal.selfReporting.approvalId)
          .then(() => {
            this.skillInternal.selfReporting.rejectedOn = null;
            this.skillInternal.selfReporting.rejectedMsg = null;
            this.skillInternal.selfReporting.requestedOn = null;
          });
      },
      reportSkill(approvalRequestedMsg) {
        this.errNotification.enable = false;
        this.errNotification.msg = '';

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
            this.selfReportModalVisible = false;
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
    },
  };
</script>

<style scoped>
</style>
