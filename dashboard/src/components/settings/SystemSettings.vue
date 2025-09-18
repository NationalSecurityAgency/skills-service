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
import { ref, onMounted } from 'vue';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import { object, string } from 'yup'
import {useForm} from "vee-validate";
import MarkdownEditor from "@/common-components/utilities/markdown/MarkdownEditor.vue";
import SettingsService from '@/components/settings/SettingsService.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

const appConfig = useAppConfig();
const timePeriodRegex = /^(PT)?(?=(?:0\.)?\d+[HMS])((?:0\.)?\d+H)?((?:0\.)?\d+M)?((?:0\.)?\d+S)?$/;

const schema = object({
  resetTokenExpiration: string()
      .required()
      .label('Token Expiration')
      .test('iso8601', 'Invalid ISO 8601 Time Duration', (value) => {
        if (value) {
          return value.match(timePeriodRegex) !== null;
        }
        return false;
      }),
  customHeader: string().label('Custom Header').max(3000).noScript(),
  customFooter: string().label('Custom Footer').max(3000).noScript(),
  userAgreement: string().label('User Agreement').noScript()
});

const { defineField, meta } = useForm({
  validationSchema: schema,
  initialValues: {
    resetTokenExpiration: '2H'
  }
})

const isSaving = ref(false);
const overallErrMsg = ref('');
const [resetTokenExpiration] = defineField('resetTokenExpiration');
const [customHeader] = defineField('customHeader');
const [customFooter] = defineField('customFooter');
const [userAgreement] = defineField('userAgreement');

onMounted(() => {
  loadSystemSettings();
})

function saveSystemSettings() {
  isSaving.value = true;
  overallErrMsg.value = '';

  let updatedTokenExpiration = resetTokenExpiration.value;
  if (!updatedTokenExpiration.toLowerCase().startsWith('pt')) {
    updatedTokenExpiration = `PT${updatedTokenExpiration}`;
  }

  SettingsService.saveSystemSettings({
    resetTokenExpiration: updatedTokenExpiration,
    customHeader: customHeader.value,
    customFooter: customFooter.value,
    userAgreement: userAgreement.value,
  }).then(() => {
    overallErrMsg.value = 'Saved!'
    appConfig.refreshConfig();
  }).catch(() => {
    overallErrMsg.value = 'Failed to Save System Settings!';
  }).finally(() => {
    isSaving.value = false;
  });
}

function loadSystemSettings() {
  SettingsService.loadSystemSettings().then((resp) => {
    if (resp) {
      if (resp.resetTokenExpiration) {
        resetTokenExpiration.value = resp.resetTokenExpiration.replace('PT', '');
      }

      if (resp.customHeader) {
        customHeader.value = resp.customHeader;
      }
      if (resp.customFooter) {
        customFooter.value = resp.customFooter;
      }
      if (resp.userAgreement) {
        userAgreement.value = resp.userAgreement;
      }
    }
  });
}
</script>

<template>
  <sub-page-header title="System Settings"/>

  <Card>
    <template #content>
      <Message v-if="overallErrMsg" :sticky="false" :life="10000">{{overallErrMsg}}</Message>

      <SkillsTextInput name="resetTokenExpiration" label="Token Expiration" is-required>
        <template #footer>
          <div class="w-full">
            <small class="text-info" id="resetTokenExpirationFormat"><i class="fas fa-exclamation-circle" aria-hidden="true" /> supports ISO 8601 time duration format, e.g., 2H, 30M, 1H30M, 1M42S, etc</small>
          </div>
        </template>
      </SkillsTextInput>
      <SkillsTextarea name="customHeader" label="Custom Header" />
      <SkillsTextarea name="customFooter" label="Custom Footer" />
      <markdown-editor class="mt-8" name="userAgreement" label="User Agreement" :allow-attachments="false"/>

      <SkillsButton label="Save" :icon="isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right'"
                    data-cy="saveSystemSettings" @click="saveSystemSettings" :disabled="!meta.valid || !meta.dirty" />
    </template>
  </Card>
</template>

<style scoped></style>
