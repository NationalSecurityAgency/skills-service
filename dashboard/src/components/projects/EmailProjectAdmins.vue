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
  <div id="contact-users-panel">
    <sub-page-header title="Contact Project Administrators" />

    <b-card body-class="p-0">
      <b-overlay :show="!emailFeatureConfigured">
        <div slot="overlay" class="alert alert-warning mt-2" data-cy="contactUsers_emailServiceWarning">
          <i class="fa fa-exclamation-triangle" aria-hidden="true"/> Please note that email notifications are currently disabled. Email configuration has not been performed on this instance of SkillTree. Please contact the root administrator.
        </div>
        <div class="m-4 h5 text-uppercase">Email Content</div>
        <div class="m-4"><b-badge variant="info" data-cy="projectAdminCount">{{this.currentCount|number}}</b-badge> Project Administrators</div>
        <div class="row pl-3 pr-3 pt-3 pb-1 m-3 mb-1">
          <b-form-group class="w-100" id="subject-line-input-group" label="Subject Line" label-for="subject-line-input" label-class="text-muted">
            <b-input class="w-100" v-model="subject" id="subject-line-input" data-cy="emailUsers_subject"/>
          </b-form-group>
        </div>
        <div class="row pl-3 pr-3 pb-1 ml-3 mr-3 mb-1 mt-1">
          <b-form-group class="w-100" id="body-input-group" label="Email Body" label-for="body-input" label-class="text-muted">
            <markdown-editor class="w-100" v-model="body" data-cy="emailUsers_body"/>
          </b-form-group>
        </div>
        <div class="row pl-3 pr-3 pb-3 pt-1 ml-3 mr-3 mb-3 mt-1">
          <b-button class="mr-3" data-cy="previewAdminEmail"
                    :disabled="isEmailDisabled"
                    @click="previewEmail"
                    variant="outline-primary" aria-label="preview email to project administrators">
            <span>Preview</span> <i :class="[emailing ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-eye']" style="font-size:1rem;" aria-hidden="true"/>
          </b-button>
          <b-button variant="outline-primary" class="mr-1" @click="emailUsers" data-cy="emailUsers-submitBtn"
                    :disabled="isEmailDisabled"><i :class="[emailing ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fas fa-mail-bulk']" /> Email</b-button>
          <transition name="fade">
            <span v-if="emailSent" class="pt-2 pl-1"><i class="far fa-check-square text-success"/> {{ sentMsg }}</span>
          </transition>
        </div>
      </b-overlay>
    </b-card>
  </div>

</template>

<script>
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import MarkdownEditor from '../utils/MarkdownEditor';
  import ProjectService from './ProjectService';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';

  export default {
    name: 'EmailAdmins',
    components: {
      SubPageHeader,
      MarkdownEditor,
    },
    mixins: [MsgBoxMixin],
    data() {
      return {
        emailFeatureConfigured: true,
        currentCount: 0,
        subject: '',
        body: '',
        emailSent: false,
        emailing: false,
        sentMsg: 'Email Sent!',
      };
    },
    mounted() {
      ProjectService.isEmailServiceSupported().then((emailEnabled) => {
        this.emailFeatureConfigured = emailEnabled;
      });
      ProjectService.countProjectAdmins().then((count) => {
        this.currentCount = count;
      });
    },
    computed: {
      isEmailDisabled() {
        return !this.body || !this.subject || this.emailing || this.emailSent || this.currentCount < 1;
      },
    },
    watch: {
    },
    methods: {
      emailUsers() {
        this.emailing = true;
        ProjectService.contactProjectAdmins({
          emailBody: this.body,
          emailSubject: this.subject,
        }).then(() => {
          this.emailSent = true;
          this.sentMsg = 'Email sent!';
          this.$nextTick(() => {
            this.body = '';
            this.subject = '';
          });
          setTimeout(() => { this.emailSent = false; }, 8000);
        }).finally(() => {
          this.emailing = false;
        });
      },
      previewEmail() {
        this.emailing = true;
        this.sentMsg = 'Preview email sent!';
        ProjectService.rootPreviewEmail({
          emailBody: this.body,
          emailSubject: this.subject,
        }).then(() => {
          this.emailSent = true;
          setTimeout(() => { this.emailSent = false; }, 8000);
        }).finally(() => {
          this.emailing = false;
        });
      },
    },
  };
</script>

<style>
  .fade-enter-active {
    transition: opacity .5s;
  }
  .fade-leave-active {
    transition: opacity 2s;
  }
  .fade-enter, .fade-leave-to {
    opacity: 0;
  }
</style>
