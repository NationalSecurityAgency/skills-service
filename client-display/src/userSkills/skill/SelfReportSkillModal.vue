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
  <b-modal id="reportSkillModal"
           title="REPORT SKILL"
           ok-title="Submit"
           :no-close-on-backdrop="true"
           v-model="modalVisible">
    <div id="reportSkillMsg" class="row p-2" data-cy="selfReportSkillMsg">
      <div class="col-auto text-center">
        <i v-if="isHonorSystem" class="fas fa-chess-knight text-success" style="font-size: 3rem"></i>
        <i v-if="isApprovalRequired" class="fas fa-thumbs-up text-info" style="font-size: 3rem"></i>
      </div>
      <div class="col">
        <p class="h5" v-if="isHonorSystem">This skill can be submitted under the <b class="text-success">Honor
          System</b> and <b class="text-success">{{ skill.pointIncrement }}</b> points will be awarded right away!
        </p>
        <p class="h5" v-if="isApprovalRequired">This skill requires <b class="text-info">approval</b>. Submit with an
          <span class="text-muted">optional</span> message and it will enter an approval queue.</p>
      </div>
    </div>
    <input type="text" id="approvalRequiredMsg" @input="validate"
           v-if="isApprovalRequired" v-model="approvalRequestedMsg"
           data-cy="selfReportMsgInput"
           aria-describedby="reportSkillMsg"
           aria-label="Optional request approval message"
           class="form-control" placeholder="Message (optional)">
    <span v-if="inputInvalid" class="text-small text-danger" data-cy="selfReportMsgInput_errMsg"><i class="fas fa-exclamation-circle"/> {{ inputInvalidExplanation }}</span>
    <template #modal-footer>
      <button type="button" class="btn btn-outline-danger text-uppercase" @click="cancel">
        <i class="fas fa-times-circle"></i> Cancel
      </button>
      <button type="button" class="btn btn-outline-success text-uppercase" @click="reportSkill"
              data-cy="selfReportSubmitBtn" :disabled="inputInvalid">
        <i class="fas fa-arrow-alt-circle-right"></i> Submit
      </button>
    </template>
  </b-modal>
</template>

<script>
  import debounce from 'lodash/debounce';
  import UserSkillsService from '../service/UserSkillsService';

  export default {
    name: 'SelfReportSkillModal',
    props: {
      isHonorSystem: Boolean,
      isApprovalRequired: Boolean,
      skill: Object,
    },
    data() {
      return {
        modalVisible: true,
        approvalRequestedMsg: '',
        inputInvalid: false,
        inputInvalidExplanation: '',
      };
    },
    methods: {
      validate: debounce(function debouncedValidate() {
        UserSkillsService.validateDescription(this.approvalRequestedMsg)
          .then((res) => {
            this.inputInvalid = !res.valid;
            this.inputInvalidExplanation = res.msg;
          });
      }, 250),
      reportSkill() {
        this.modalVisible = false;
        this.$emit('report-skill', this.approvalRequestedMsg);
      },
      cancel() {
        this.modalVisible = false;
        this.$emit('cancel');
      },
    },
  };
</script>

<style scoped>

</style>
