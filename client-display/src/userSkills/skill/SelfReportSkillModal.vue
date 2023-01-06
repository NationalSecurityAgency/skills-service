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
  <ValidationObserver ref="observer" v-slot="{invalid, handleSubmit}" slim>
    <b-modal id="reportSkillModal"
             :title="`REPORT ${skillDisplayName.toUpperCase()}`"
             ok-title="Submit"
             size="xl"
             :no-close-on-backdrop="true"
             v-model="modalVisible"
             @hide="cancel">
      <modal-positioner :y-offset="modalYOffset"/>
      <div id="reportSkillMsg" class="row p-2" data-cy="selfReportSkillMsg">
        <div class="col-auto text-center">
          <i v-if="isHonorSystem" class="fas fa-chess-knight text-success" style="font-size: 3rem"></i>
          <i v-if="isApprovalRequired" class="fas fa-thumbs-up text-info" style="font-size: 3rem"></i>
        </div>
        <div class="col">
          <p class="h5" v-if="isHonorSystem">This {{ skillDisplayName.toLowerCase() }} can be submitted under the <b class="text-success">Honor
            System</b> and <b class="text-success">{{ skill.pointIncrement }}</b> points will be awarded right away!
          </p>
          <p class="h5" v-if="isApprovalRequired">This {{ skillDisplayName.toLowerCase() }} requires <b class="text-info">approval</b>. Submit with {{ isJustitificationRequired ? 'a' : 'an' }}
            <span v-if="!isJustitificationRequired" class="text-muted">optional</span> justification and it will enter an approval queue.</p>
        </div>
      </div>
      <div class="row">
        <div class="col-12">
          <ValidationProvider rules="maxDescriptionLength|customDescriptionValidator" :debounce="250" v-slot="{ errors }" name="Approval Justification">
            <markdown-editor v-if="showDescription" class="form-text"
                             id="approvalRequiredMsg"
                             v-model="approvalRequestedMsg"
                             data-cy="selfReportMsgInput"
                             aria-describedby="reportSkillMsg"
                             :aria-label="isJustitificationRequired ? 'Optional request approval justification' : 'Required request approval justification'"
                             :placeholder="`Justification (${isJustitificationRequired ? 'required' : 'optional'})`"
                             :resizable="true"
                             aria-errormessage="approvalMessageError"
                             :aria-invalid="errors && errors.length > 0"/>
            <small role="alert" id="approvalMessageError" class="form-text text-danger" data-cy="selfReportMsgInput_errMsg">{{ errors[0] }}</small>
          </ValidationProvider>
        </div>
      </div>

      <template #modal-footer>
        <button type="button" class="btn btn-outline-danger text-uppercase" @click="cancel">
          <i class="fas fa-times-circle"></i> Cancel
        </button>
        <button type="button" class="btn btn-outline-success text-uppercase" @click="handleSubmit(reportSkill)"
                data-cy="selfReportSubmitBtn" :disabled="invalid || !messageValid">
          <i class="fas fa-arrow-alt-circle-right"></i> Submit
        </button>
      </template>
    </b-modal>
  </ValidationObserver>
</template>

<script>
  import debounce from 'lodash/debounce';
  import MarkdownEditor from '@/common-components/utilities/MarkdownEditor';
  import UserSkillsService from '../service/UserSkillsService';
  import ModalPositioner from './ModalPositioner';

  export default {
    name: 'SelfReportSkillModal',
    components: { ModalPositioner, MarkdownEditor },
    props: {
      isHonorSystem: Boolean,
      isApprovalRequired: Boolean,
      isJustitificationRequired: Boolean,
      skill: Object,
    },
    data() {
      return {
        modalVisible: true,
        approvalRequestedMsg: '',
        modalYOffset: 0,
        showDescription: false,
      };
    },
    mounted() {
      setTimeout(() => {
        // this.showDescription = true;
      }, '100');
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
        this.modalVisible = false;
        this.$emit('report-skill', this.approvalRequestedMsg);
      },
      cancel() {
        this.modalVisible = false;
        this.$emit('cancel');
      },
      updatePosition(yOffset) {
        this.modalYOffset = yOffset;
        this.$nextTick(() => {
          // this.showDescription = true;
        });
      },
    },
  };
</script>

<style scoped>

</style>
