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
  <b-card style="min-height: 330px;">
      <ValidationObserver ref="observer" v-slot="{invalid, handleSubmit}" slim>
          <div :id="`reportSkillMsg-${skill.skillId}`" class="text-left mb-2 skills-theme-primary-color" data-cy="selfReportSkillMsg">
             ** Submit with {{ isJustitificationRequired ? 'a' : 'an' }}
                <span v-if="!isJustitificationRequired" class="text-muted">optional</span> justification and it will enter an approval queue.
          </div>
          <div class="row">
            <div class="col-12">
              <ValidationProvider rules="maxDescriptionLength|customDescriptionValidator" :debounce="250" v-slot="{ errors }" name="Approval Justification">
                <markdown-editor class="form-text"
                                 :id="`approvalRequiredMsg-${skill.skillId}`"
                                 ref="approvalRequiredMsg"
                                 v-model="approvalRequestedMsg"
                                 :project-id="skill.projectId"
                                 :skill-id="skill.skillId"
                                 data-cy="selfReportMsgInput"
                                 :aria-describedby="`reportSkillMsg-${skill.skillId}`"
                                 markdownHeight="250px"
                                 label="Justification"
                                 :show-label="false"
                                 :aria-label="isJustitificationRequired ? 'Optional request approval justification' : 'Required request approval justification'"
                                 :placeholder="`Justification (${isJustitificationRequired ? 'required' : 'optional'})`"
                                 :resizable="true"
                                 aria-errormessage="approvalMessageError"
                                 :aria-invalid="errors && errors.length > 0"/>
                <small role="alert" id="approvalMessageError" class="form-text text-danger" data-cy="selfReportMsgInput_errMsg">{{ errors[0] }}</small>
              </ValidationProvider>
            </div>
          </div>

          <div class="text-center mt-2">
            <button type="button"
                    class="btn btn-outline-danger text-uppercase mr-1 skills-theme-btn"
                    data-cy="selfReportApprovalCancelBtn"
                    @click="cancel">
              <i class="fas fa-times-circle"></i> Cancel
            </button>
            <button type="button"
                    class="btn btn-outline-success text-uppercase skills-theme-btn"
                    @click="handleSubmit(reportSkill)"
                    data-cy="selfReportSubmitBtn" :disabled="invalid || !messageValid">
              <i class="fas fa-arrow-alt-circle-right"></i> Request
            </button>
          </div>
      </ValidationObserver>
    </b-card>
</template>

<script>
  import debounce from 'lodash/debounce';
  import MarkdownEditor from '@/common-components/utilities/MarkdownEditor';
  import UserSkillsService from '../service/UserSkillsService';

  export default {
    name: 'JustificationInput',
    components: { MarkdownEditor },
    props: {
      isHonorSystem: Boolean,
      isApprovalRequired: Boolean,
      isJustitificationRequired: Boolean,
      skill: Object,
    },
    data() {
      return {
        approvalRequestedMsg: '',
      };
    },
    computed: {
      messageValid() {
        if (this.isJustitificationRequired && (!this.approvalRequestedMsg || this.approvalRequestedMsg.length <= 0)) {
          return false;
        }
        return true;
      },
      charactersRemaining() {
        return this.$store.getters.config.maxSelfReportMessageLength - this.approvalRequestedMsg.length;
      },
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
        this.$emit('report-skill', this.approvalRequestedMsg);
      },
      cancel() {
        this.$emit('cancel');
      },
      focusOnMarkdownEditor() {
        this.$refs.approvalRequiredMsg.focusOnMarkdownEditor();
      },
    },
  };
</script>

<style scoped>

</style>
