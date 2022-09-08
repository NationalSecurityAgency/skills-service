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
           :title="`Contact ${this.projectName}`"
           ok-title="Submit"
           :no-close-on-backdrop="true"
           @hide="cancel"
           :busy="sending"
           :ok-disabled="msgInvalid || !contactOwnersMsg || sendComplete"
           @ok="contactOwners"
           header-bg-variant="info"
           header-text-variant="light"
           no-fade
           role="dialog"
           data-cy="contactProjectOwnerDialog"
           v-model="modalVisible"
          size="xl">
    <loading-container v-bind:is-loading="sending">
      <div v-if="!sendComplete" id="contactOwnersMsg" class="row pt-2 pb-2 pl-4 pr-4" data-cy="contactOwnersMsg">
        <b-form-textarea type="text" id="approvalRequiredMsg"
                      v-model="contactOwnersMsg"
                       rows="5"
                       data-cy="contactOwnersMsgInput"
                       :aria-label="'Contact Project Owners'"
                       class="form-control"/>
        <div :class="{ 'float-right':true, 'text-small': true, 'text-danger': charactersRemaining < 0 }" data-cy="charactersRemaining">{{ charactersRemaining }} characters remaining <i v-if="charactersRemaining < 0" class="fas fa-exclamation-circle"/></div>
        <span v-if="msgInvalid" class="text-small text-danger" data-cy="contactOwnersInput_errMsg"><i class="fas fa-exclamation-circle"/> {{ msgInvalidMsg }}</span>
      </div>
      <div v-if="sendComplete" data-cy="contactOwnerSuccessMsg">
        <p class="text-center text-success"><i class="fa fa-check" /> Message sent!</p>
        <p class="text-center">The Project Administrator(s) of {{ projectName }} will be notified of your question via email.</p>
      </div>
    </loading-container>
    <template #modal-footer="{ ok, cancel }">
      <div class="w-100">
        <b-button ref="okButton" size="sm" variant="success" class="float-right ml-2" @click="ok()"
                data-cy="contactOwnersSubmitBtn" :disabled="!messageValid || !contactOwnersMsg || sending">
          <i class="fas fa-arrow-alt-circle-right"></i> {{ sendComplete ? 'Ok' : 'Submit' }}
        </b-button>
        <b-button v-if="!sendComplete" size="sm" variant="secondary"
                  class="float-right" @click="cancel()" data-cy="cancelBtn">
          <i class="fas fa-times-circle"></i> Cancel
        </b-button>
      </div>
    </template>
  </b-modal>
</template>

<script>

  import debounce from 'lodash.debounce';
  import MyProgressService from '@/components/myProgress/MyProgressService';
  import LoadingContainer from '@/components/utils/LoadingContainer';
  import CustomValidationService from '../../validators/CustomValidatorsService';

  export default {
    name: 'ContactOwnersDialog',
    props: {
      projectName: {
        type: String,
        required: true,
      },
      value: {
        type: Boolean,
        required: true,
      },
      projectId: {
        type: String,
        required: true,
      },
    },
    components: {
      LoadingContainer,
    },
    data() {
      return {
        modalVisible: this.value,
        contactOwnersMsg: '',
        msgInvalid: false,
        msgInvalidMsg: '',
        sending: false,
        sendComplete: false,
      };
    },
    watch: {
      modalVisible(newValue) {
        this.$emit('input', newValue);
      },
      contactOwnersMsg(newValue) {
        if (newValue?.length > 0) {
          this.validate();
        }
      },
    },
    computed: {
      messageValid() {
        if (this.msgInvalid) {
          return false;
        }

        const maxLength = this.$store.getters.config ? this.$store.getters.config.maxContactOwnersMessageLength : -1;
        if (maxLength === -1) {
          return true;
        }

        return this.charactersRemaining >= 0;
      },
      charactersRemaining() {
        return this.$store.getters.config.maxContactOwnersMessageLength - this.contactOwnersMsg.length;
      },
    },
    methods: {
      contactOwners(e) {
        if (!this.sendComplete) {
          e.preventDefault();
          this.sending = true;
          MyProgressService.contactOwners(this.projectId, this.contactOwnersMsg).then(() => {
            this.sending = false;
            this.sendComplete = true;
            this.$nextTick(() => {
              this.$announcer.polite(`Message has been sent to owners of project ${this.projectName}`);
              this.$refs.okButton.focus();
            });
          });
        }
      },
      cancel(e) {
        if (e.trigger !== 'ok' && !this.sendComplete) {
          this.modalVisible = false;
          this.$emit('hidden', { ...e });
        }
      },
      validate: debounce(function debouncedValidate() {
        CustomValidationService.validateDescription(this.contactOwnersMsg)
          .then((res) => {
            this.msgInvalid = !res.valid;
            this.msgInvalidMsg = res.msg;
          });
      }, 250),
    },
  };
</script>

<style scoped>

</style>
