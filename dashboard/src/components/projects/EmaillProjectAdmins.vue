/*
Copyright 2024 SkillTree

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
<script setup>
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'
import { computed, onMounted, ref, nextTick } from 'vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import ProjectService from '@/components/projects/ProjectService.js'
import MarkdownEditor from "@/common-components/utilities/markdown/MarkdownEditor.vue";
import { object, string } from 'yup'
import { useForm } from 'vee-validate'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useRoute } from 'vue-router'

const route = useRoute()
const appInfo = useAppInfoState()
const appConfig = useAppConfig()
const numFormat = useNumberFormat()
const announcer = useSkillsAnnouncer()

const emailFeatureConfigured = computed(() => { return appInfo.emailEnabled });

const currentCount = ref(0)
const emailSent = ref(false);
const emailing = ref(false);
const sentMsg = ref('');

onMounted(() => {
  ProjectService.countProjectAdmins().then((count) => {
    currentCount.value = count;
  });
})

const schema = object({
  subjectLine: string().required().max(appConfig.descriptionMaxLength).customDescriptionValidator('Subject Line', false).label('Subject Line'),
  emailBody: string().required().max(appConfig.descriptionMaxLength).customDescriptionValidator('Email Body', false).label('Email Body')
});

const { defineField, meta } = useForm({
  validationSchema: schema,
})

const [emailBody] = defineField('emailBody');
const [subjectLine] = defineField('subjectLine');

const isEmailDisabled = computed(() => {
  return !emailBody.value || !subjectLine.value || emailing.value || emailSent.value || currentCount.value < 1;
});

const isPreviewDisabled = computed(() => {
  return !emailBody.value || !subjectLine.value;
});

const previewEmail = () => {
  emailing.value = true;
  sentMsg.value = 'Preview email sent!';
  const params = {
    emailBody: emailBody.value,
    emailSubject: subjectLine.value,
  }
  ProjectService.rootPreviewEmail(params).then(() => {
    emailSent.value = true;
    announcer.polite('Preview Email has been sent');
    setTimeout(() => { emailSent.value = false; }, 8000);
  }).finally(() => {
    emailing.value = false;
  });
};

const emailUsers = () => {
  emailing.value = true;
  ProjectService.contactProjectAdmins({
    emailBody: emailBody.value,
    emailSubject: subjectLine.value,
  }).then(() => {
    emailSent.value = true;
    sentMsg.value = 'Email sent!';
    nextTick(() => {
      emailBody.value = '';
      subjectLine.value = '';
    });
    setTimeout(() => { emailSent.value = false; }, 8000);
  }).finally(() => {
    emailing.value = false;
  });
}
</script>

<template>
  <div id="contact-users-panel">
    <sub-page-header title="Contact Project Administrators" :title-level="1" />

    <Card>
      <template #content>
        <Message severity="warn" data-cy="contactUsers_emailServiceWarning" v-if="!emailFeatureConfigured" :closable="false">
          Please note that email notifications are currently disabled. Email configuration has not been performed on this instance of SkillTree. Please contact the root administrator.
        </Message>

        <BlockUI :blocked="!emailFeatureConfigured" :class="{'p-3': !emailFeatureConfigured}">
          <div>
            <Tag data-cy="projectAdminCount">{{ numFormat.pretty(currentCount) }}</Tag>
            Project Administrators
          </div>
          <h2 class="uppercase text-xl my-6">Email Content</h2>
          <div>
            <SkillsTextInput name="subjectLine" label="Subject Line" data-cy="emailUsers_subject" class="w-full" />
            <markdown-editor data-cy="emailUsers_body"
                             class="mt-2"
                             label="Email Body"
                             name="emailBody"
                             :resizable="true"
                             :allow-attachments="false"
                             :use-html="true"/>
          </div>
          <div class="mt-2 flex gap-2">
            <SkillsButton data-cy="previewAdminEmail"
                          :disabled="isPreviewDisabled || !meta.valid"
                          @click="previewEmail"
                          label="Preview"
                          :icon="emailing ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-eye'"
                          aria-label="preview email to project users" />
            <SkillsButton @click="emailUsers" data-cy="emailUsers-submitBtn"
                          label="Email"
                          :icon="emailing ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fas fa-mail-bulk'"
                          :disabled="isEmailDisabled || !meta.valid" />
            <transition name="fade">
              <InlineMessage v-if="emailSent" data-cy="emailSent" severity="success" class="ml-2">{{ sentMsg }}</InlineMessage>
            </transition>
          </div>
        </BlockUI>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>