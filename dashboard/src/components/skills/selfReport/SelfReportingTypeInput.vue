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
      <b-form-checkbox data-cy="selfReportEnableCheckbox" id="self-report-checkbox"
                       class="d-inline" v-model="selfReport.enabled"
                       v-on:input="updatedSelfReportingStatus"/>
      Self Reporting <inline-help msg="Check to enable self-reporting of this skill by users"/>:
    </div>
    <div class="col-12 col-lg-auto">
      <b-form-group v-slot="{ ariaDescribedby }" class="m-0 p-0">
        <b-form-radio-group
          id="self-reporting-type"
          v-model="selfReport.selected"
          :options="selfReport.options"
          :aria-describedby="ariaDescribedby"
          name="Self Reporting Options"
          data-cy="selfReportTypeSelector"
        ></b-form-radio-group>
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

  export default {
    name: 'SelfReportingTypeInput',
    components: { InlineHelp },
    props: {
      value: String,
      skill: Object,
      isEdit: Boolean,
    },
    data() {
      return {
        selfReport: {
          originalSelfReportingType: this.skill.selfReportingType,
          loading: true,
          enabled: false,
          selected: 'Approval',
          options: [
            { text: 'Approval Queue', value: 'Approval', disabled: true },
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
    },
  };
</script>

<style scoped>

</style>
