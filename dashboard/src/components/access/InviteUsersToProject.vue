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

  <loading-container :is-loading="loading" class="w-100">
    <div class="">
      <div class="mb-3" data-cy="inviteExpiration">
        <label class="text-secondary" id="inviteExpirationLabel">
          Invite Expiration:
          <inline-help
            target-id="inviteExpirationHelp"
            msg="How long the project invite will be valid for before it expires."/>
        </label>
        <b-form-select v-model="expirationTime"
                       :options="expirationOptions"
                       aria-labelledby="inviteExpirationLabel"
                       data-cy="inviteExpirationSelect" required/>
      </div>

      <div class="mb-3" data-cy="inviteEmail">
        <label class="text-secondary" id="inviteEmailLabel">
          Email Addresses:
          <inline-help
            target-id="emailAddresesHelp"
            msg="Email Addresses of users to invite to access this project. Must be unique as each email address will be sent a one-time use invite token. Comma separated, semi-colon separated, and one email per line input formats are supported."/>
        </label>
        <b-form-textarea v-model="currentEmails"
                         rows="5"
                         debounce="300"
                         @input="clearInvalid"
                      aria-labelledby="inviteEmailLabel"
                      data-cy="inviteEmailInput" required/>
        <b-button @click="addEmails" variant="outline-info" class="mt-2" data-cy="addEmails"
                  :disabled="!hasEmails" aria-label="Add email addresses to the list of recipients">
          Add Recipients <i class="fas fa-plus-circle"/>
        </b-button>
        <span v-if="tooManyEmails" data-cy="maxEmailRecipients" class="text-danger ml-3" role="alert">
          Only {{ maxEmails }} recipients can be invited at one time
        </span>
        <span v-if="invalidEmails" class="text-danger ml-3" data-cy="invalidEmails" role="alert">
          Unable to add the following invalid email recipients: {{ invalidEmails }}
        </span>

      </div>

      <div class="mb-3" data-cy="inviteRecipients">
        <b-badge v-for="(email) of inviteRecipients" :key="email" variant="info"
                 class="pl-2 m-2 text-break"
                 style="max-width: 85%;" data-cy="inviteRecipient">
          {{ email }}
          <b-button @click="removeRecipient(email)"
                    variant="outline-info" size="sm" class="text-warning"
                    :aria-label="`Remove invite recipient ${email}`"
                    data-cy="inviteRecipient-removeBtn"><i class="fa fa-trash"/><span class="sr-only">delete recipient {{ email }}</span>
          </b-button>
        </b-badge>
      </div>
      <hr />
      <b-button @click="sendInvites"
                :disabled="sendInviteDisabled"
                variant="outline-info"
                :aria-label="`send project invites to ${inviteRecipients.length} users`"
                data-cy="sendInvites-btn">
          Send Invites <i :class="sendInviteIcon" />
      </b-button> <span v-if="showSuccessMsg" class="text-success ml-2" data-cy="invitationsSentAlert">
              <i class="fa fa-check" />
              {{ successMsg }}
            </span>
      <div v-if="failedEmails" class="mt-2 alert alert-danger" role="alert" data-cy="failedEmails">
        <div>
          <i class="fas fa-exclamation-triangle" aria-hidden="true"/> Unable to send invites to:
        </div>
        <div v-for="failedEmail in failedEmails" :key="failedEmail" class="pl-1">
          - {{ failedEmail }}
        </div>
      </div>
    </div>
  </loading-container>

</template>

<script>

  import InlineHelp from '@/components/utils/InlineHelp';
  import LoadingContainer from '@/components/utils/LoadingContainer';
  import AccessService from '@/components/access/AccessService';
  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';

  const validEmail = /^[a-z0-9.]{1,64}@[a-z0-9.]{1,64}$/i;
  const stripNames = /<([^\s<>@]+@[^\s<>@]+)>/;
  export default {
    name: 'InviteUsersToProject',
    props: {
      projectId: {
        required: true,
        type: String,
      },
    },
    components: { InlineHelp, LoadingContainer },
    mixins: [MsgBoxMixin],
    data() {
      return {
        loading: false,
        expirationTime: 'PT8H',
        currentEmails: '',
        invalidEmails: '',
        maxRecipients: 50,
        sending: false,
        showSuccessMsg: false,
        successMsg: '',
        inviteRecipients: [],
        failedEmails: '',
        expirationOptions: [
          { value: 'PT30M', text: '30 minutes' },
          { value: 'PT8H', text: '8 hours' },
          { value: 'PT24H', text: '24 hours' },
          { value: 'P7D', text: '7 days' },
          { value: 'P30D', text: '30 days' },
        ],
      };
    },
    computed: {
      hasEmails() {
        return this.currentEmails.length > 0;
      },
      sendInviteDisabled() {
        return this.inviteRecipients.length === 0 || this.inviteRecipients.length > this.maxRecipients || !this.expirationTime || this.sending;
      },
      sendInviteIcon() {
        return this.sending ? 'fas fa-spinner' : 'fas fa-paper-plane';
      },
      tooManyEmails() {
        return this.inviteRecipients.length >= this.maxEmails;
      },
      maxEmails() {
        return this.$store.getters.config.maxProjectInviteEmails;
      },
    },
    methods: {
      removeRecipient(email) {
        const idx = this.inviteRecipients.indexOf(email);
        this.inviteRecipients.splice(idx, 1);
      },
      addEmails() {
        if (this.currentEmails.length > 0) {
          const potentialEmails = this.splitCurrentEmails();
          const invalid = [];
          let successful = 0;

          const maxReached = potentialEmails.filter((pmail) => {
            if (this.tooManyEmails) {
              return true;
            }
            let email = this.removeName(pmail);
            email = email.trim();
            const isValid = this.isValidEmail(email);
            if (!isValid) {
              invalid.push(pmail);
            } else if (!this.inviteRecipients.find((invited) => invited === email)) {
              successful += 1;
              this.inviteRecipients.push(email);
            }
            return false;
          });

          this.failedEmails = '';

          if (successful > 0) {
            this.$nextTick(() => this.$announcer.polite(`added ${successful} project invite email recipients`));
          }
          if (invalid.length > 0) {
            this.invalidEmails = invalid.join(', ');
          }
          if (maxReached.length > 0 || invalid.length > 0) {
            this.currentEmails = [...invalid, ...maxReached].join('\n');
          } else {
            this.currentEmails = '';
          }
        }
      },
      clearInvalid() {
        this.invalidEmails = '';
      },
      removeName(pmail) {
        if (stripNames.test(pmail)) {
          return pmail.match(stripNames)[1];
        }
        return pmail;
      },
      splitCurrentEmails() {
        return this.currentEmails.split(/;|,|\r?\n/);
      },
      isValidEmail(pmail) {
        return validEmail.test(pmail);
      },
      sendInvites() {
        this.failedEmails = '';
        this.sending = true;

        const inviteRequest = {
          validityDuration: this.expirationTime,
          recipients: this.inviteRecipients,
        };

        AccessService.sendProjectInvites(this.projectId, inviteRequest).then((resp) => {
          this.inviteRecipients = [];
          if (resp.unsuccessful) {
            this.failedEmails = resp.unsuccessful;
            if (this.currentEmails) {
              this.currentEmails += `\n${resp.unsuccessful.join('\n')}`;
            } else {
              this.currentEmails += resp.unsuccessful.join('\n');
            }
          }
          if (resp.successful.length > 0) {
            this.successMsg = `Successfully sent ${resp.successful.length} project invites`;
            this.showSuccessMsg = true;
            setTimeout(() => {
              this.showSuccessMsg = false;
            }, 4000);
            this.$emit('invites-sent');
          }
        }).finally(() => {
          this.sending = false;
        });
      },
      canDiscard() {
        return new Promise((resolve) => {
          const hasRecipients = this.inviteRecipients.length > 0;
          if (this.currentEmails !== '' || hasRecipients) {
            if (hasRecipients) {
              this.msgConfirm('There are invite recipients who have not been sent an invitation, '
                + 'are you sure you wish to leave this page?', 'Discard Recipients?', 'Let\'s Go!')
                .then((ok) => {
                  if (ok) {
                    resolve(true);
                  }
                  resolve(false);
                });
            } else {
              const current = JSON.stringify(this.splitCurrentEmails().sort());
              const failed = JSON.stringify(this.failedEmails.split(', ')?.sort());
              const invalid = JSON.stringify(this.invalidEmails.split(', ')?.sort());
              if (current === failed || current === invalid) {
                resolve(true);
              } else {
                this.msgConfirm('There are emails that haven\'t been added as a recipient or sent an invite,'
                  + ' are you sure you wish to leave this page?', 'Discard Emails?', 'Let\'s Go!')
                  .then((ok) => {
                    let res = false;
                    if (ok) {
                      res = true;
                    }
                    resolve(res);
                  });
              }
            }
          } else {
            resolve(true);
          }
        });
      },
    },
  };
</script>

<style scoped>

</style>
