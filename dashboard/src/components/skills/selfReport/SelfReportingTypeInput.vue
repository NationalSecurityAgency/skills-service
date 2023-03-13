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
  <div class="row mb-2">
    <div class="col-12 col-lg-auto">
      <label for="self-report-checkbox">
      <b-form-checkbox data-cy="selfReportEnableCheckbox" id="self-report-checkbox"
                       class="d-inline" v-model="selfReport.enabled"
                       v-on:input="updatedSelfReportingStatus"/>
      Self Reporting <inline-help msg="Check to enable self-reporting of this skill by users"
                                  target-id="selfReportHelp"
                                  :next-focus-el="nextFocusEl"
                                  @shown="tooltipShown"
                                  @hidden="tooltipHidden"/>
      </label>
    </div>
    <div class="col-12 col-lg">
      <b-form-group v-slot="{ ariaDescribedby }" class="m-0 p-0">
        <b-form-radio-group
          id="self-reporting-type"
          v-model="selfReport.selected"
          :options="selfReport.options"
          :aria-describedby="ariaDescribedby"
          name="Self Reporting Options"
          aria-label="Self Reporting Options"
          data-cy="selfReportTypeSelector"
          stacked
        >
          <template #first>
            <div class="row m-0">
              <b-form-radio class="mr-2" value="Approval" :disabled="!selfReport.enabled">Approval Queue</b-form-radio>
              <span class="text-muted mr-3 ml-2">|</span>
              <label for="self-report-checkbox" class="m-0">
                <b-form-checkbox data-cy="justificationRequiredCheckbox" id="justification-required-checkbox"
                                 class="d-inline" v-model="selfReport.justificationRequired"
                                 :disabled="!approvalSelected || !selfReport.enabled" @input="justificationRequiredChanged"/>
                <span class="font-italic">Justification Required </span><inline-help
                                          msg="Check to require users to submit a justification when self-reporting this skill"
                                          target-id="justificationRequired"
                                          :next-focus-el="nextFocusEl"
                                          @shown="tooltipShown"
                                          @hidden="tooltipHidden"/>
              </label>
            </div>
          </template>
          <template>
            <div class="row m-0 no-gutters">
              <div class="col-12 col-lg-auto">
                <b-form-radio class="" value="Quiz" :disabled="!selfReport.enabled" data-cy="quizRadio">Quiz/Survey</b-form-radio>
              </div>
              <div class="col pl-2">
                <quiz-selector v-if="selfReport.enabled && quizSelected" :initiallySelectedQuizId="skill.quizId" @changed="quizIdSelected" />
              </div>
            </div>
          </template>
        </b-form-radio-group>
      </b-form-group>
    </div>
    <div class="col-12 pt-1" v-if="selfReport.approvals.showWarning" data-cy="selfReportingTypeWarning">
      <div class="alert alert-info" v-if="selfReport.approvals.newSelfReportingType === 'HonorSystem'">
        <i class="fas fa-exclamation-triangle mr-2"></i> Switching this skill to the <i>Honor System</i> will automatically:
        <ul>
          <li v-if="selfReport.approvals.numPending > 0">
            Approve <b>{{ selfReport.approvals.numPending | number }} pending</b> request<span v-if="selfReport.approvals.numPending>1">s</span>
          </li>
          <li v-if="selfReport.approvals.numRejected > 0">
            Remove <b>{{ selfReport.approvals.numRejected}} rejected</b> request<span v-if="selfReport.approvals.numRejected>1">s</span>
          </li>
        </ul>
      </div>
      <div class="alert alert-info" v-if="selfReport.approvals.newSelfReportingType === 'Disabled'">
        <i class="fas fa-exclamation-triangle mr-2"></i> Disabling <i>Self Reporting</i> will automatically:
        <ul>
          <li v-if="selfReport.approvals.numPending > 0">
            Remove <b>{{ selfReport.approvals.numPending | number }} pending</b> request<span v-if="selfReport.approvals.numPending>1">s</span>
          </li>
          <li v-if="selfReport.approvals.numRejected > 0">
            Remove <b>{{ selfReport.approvals.numRejected}} rejected</b> request<span v-if="selfReport.approvals.numRejected>1">s</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script>
  import SelfReportService from '@/components/skills/selfReport/SelfReportService';
  import InlineHelp from '@/components/utils/InlineHelp';
  import QuizSelector from '@/components/skills/selfReport/QuizSelector';

  export default {
    name: 'SelfReportingTypeInput',
    components: { QuizSelector, InlineHelp },
    props: {
      value: String,
      skill: Object,
      isEdit: Boolean,
      nextFocusEl: HTMLElement,
    },
    data() {
      return {
        selfReport: {
          originalSelfReportingType: this.skill.selfReportingType,
          loading: true,
          enabled: false,
          justificationRequired: false,
          selected: 'Approval',
          options: [
            // { text: 'Approval Queue', value: 'Approval', disabled: true },
            { text: 'Honor System', value: 'HonorSystem', disabled: true },
          ],
          approvals: {
            showWarning: false,
            loading: false,
            numPending: 0,
            numRejected: 0,
            newSelfReportingType: null,
          },
        },
      };
    },
    mounted() {
      if (!this.isDisabled(this.value)) {
        this.updatedSelfReportingStatus(true);
        this.selfReport.selected = this.value;
      }
      this.selfReport.originalSelfReportingType = this.skill.selfReportingType;
      this.selfReport.justificationRequired = this.skill.justificationRequired;
    },
    watch: {
      'selfReport.selected': function selfReportSelectionChanged() {
        this.selectionChanged();
      },
    },
    methods: {
      isDisabled(val) {
        return !val || val === 'Disabled';
      },
      updatedSelfReportingStatus(checked) {
        this.selfReport.enabled = checked;
        this.selfReport.options = this.selfReport.options.map((item) => {
          const copy = { ...item };
          copy.disabled = !checked;
          return copy;
        });
        this.handleSelfReportingWarning();
        if (!checked) {
          this.$emit('input', 'Disabled');
        } else {
          this.$emit('input', this.selfReport.selected);
        }
      },
      selectionChanged() {
        this.handleSelfReportingWarning();
        this.$emit('input', this.selfReport.selected);
      },
      justificationRequiredChanged(justificationRequired) {
        this.$emit('justificationRequiredChanged', justificationRequired);
      },
      quizIdSelected(quizId) {
        this.$emit('quizIdChanged', quizId);
      },
      handleSelfReportingWarning() {
        if (this.isEdit) {
          this.selfReport.approvals.showWarning = false;
          if (this.selfReport.originalSelfReportingType === 'Approval' && (this.selfReport.selected === 'HonorSystem' || !this.selfReport.enabled)) {
            this.selfReport.approvals.loading = true;
            SelfReportService.getSkillApprovalsStats(this.skill.projectId, this.skill.skillId)
              .then((res) => {
                const pendingApprovalsRes = res.find((item) => item.value === 'SkillApprovalsRequests');
                if (pendingApprovalsRes) {
                  this.selfReport.approvals.numPending = pendingApprovalsRes.count;
                }

                const pendingRejectionsRes = res.find((item) => item.value === 'SkillApprovalsRejected');
                if (pendingRejectionsRes) {
                  this.selfReport.approvals.numRejected = pendingRejectionsRes.count;
                }

                this.selfReport.approvals.showWarning = pendingApprovalsRes.count > 0 || pendingRejectionsRes.count > 0;
              })
              .finally(() => {
                this.selfReport.approvals.loading = false;
              });
            this.selfReport.approvals.newSelfReportingType = !this.selfReport.enabled ? 'Disabled' : this.selfReport.selected;
          } else {
            this.selfReport.approvals.showWarning = false;
          }
        }
      },
      tooltipShown(e) {
        this.$emit('shown', e);
      },
      tooltipHidden(e) {
        this.$emit('hidden', e);
      },
    },
    computed: {
      approvalSelected() {
        return this.selfReport.selected === 'Approval';
      },
      quizSelected() {
        return this.selfReport.selected === 'Quiz';
      },
    },
  };
</script>

<style scoped>

</style>
