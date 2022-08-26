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
  <b-modal id="contactProjectOwners"
           :title="`Contact ${this.skillDisplayName.toUpperCase()} Owners`"
           ok-title="Submit"
           :no-close-on-backdrop="true"
           v-model="modalVisible">
    <div id="contactOwnersMsg" class="row p-2" data-cy="contactOwnersMsg">
    <b-form-textarea type="text" id="approvalRequiredMsg" @input="validate"
                     v-if="isApprovalRequired" v-model="approvalRequestedMsg"
                     rows="2"
                     data-cy="selfReportMsgInput"
                     aria-describedby="reportSkillMsg"
                     :aria-label="isJustitificationRequired ? 'Optional request approval message' : 'Required request approval message'"
                     class="form-control" :placeholder="`Message (${isJustitificationRequired ? 'required' : 'optional'})`"/>
    <div v-if="isApprovalRequired" :class="{ 'float-right':true, 'text-small': true, 'text-danger': charactersRemaining < 0 }" data-cy="charactersRemaining">{{charactersRemaining}} characters remaining <i v-if="charactersRemaining < 0" class="fas fa-exclamation-circle"/></div>
    <span v-if="inputInvalid" class="text-small text-danger" data-cy="selfReportMsgInput_errMsg"><i class="fas fa-exclamation-circle"/> {{ inputInvalidExplanation }}</span>
    <template #modal-footer>
      <button type="button" class="btn btn-outline-danger text-uppercase" @click="cancel">
        <i class="fas fa-times-circle"></i> Cancel
      </button>
      <button type="button" class="btn btn-outline-success text-uppercase" @click="reportSkill"
              data-cy="selfReportSubmitBtn" :disabled="!messageValid">
        <i class="fas fa-arrow-alt-circle-right"></i> Submit
      </button>
    </template>
  </b-modal>
</template>

<script>
  export default {
    name: 'ContactOwnersDialog',
    data() {
      return {
        modalVisible: false,
      };
    }
  };
</script>

<style scoped>

</style>
