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
import { ref, computed } from 'vue';
import { object, string, boolean, number } from 'yup';
import {useForm} from "vee-validate";
import SettingsService from '@/components/settings/SettingsService.js';

const props = defineProps({
  emailSettings: Object
});

const schema = object({
  publicUrl: string().required().label('Public URL'),
  fromEmail: string().required().label('From Email').email(),
  host: string().required().label('Host'),
  port: number().required().min(0).max(65535).label('Port'),
  protocol: string().required().label('Protocol'),
  tlsEnabled: boolean(),
  authEnabled: boolean(),
  username: string().label('Username').when(['authEnabled'],  {
    is: (authEnabled) => authEnabled === true,
    then: (sch) => sch.required(),
    otherwise: (sch) => sch.notRequired()
  }),
  password: string().label('Password').when(['authEnabled'],  {
    is: (authEnabled) => authEnabled === true,
    then: (sch) => sch.required(),
    otherwise: (sch) => sch.notRequired()
  }),
});

const { values, defineField, errors, meta } = useForm({
  validationSchema: schema,
  initialValues: props.emailSettings
})

const [tlsEnabled] = defineField('tlsEnabled');
const [authEnabled] = defineField('authEnabled');
const [username] = defineField('username');
const [password] = defineField('password');

const isTesting = ref(false);
const isSaving = ref(false);
const connectionError = ref('');
const testFailed = ref(false);
const testSuccess = ref(false);

let testButtonClass = computed(() => {
  if (isTesting.value) {
    return 'fa fa-circle-notch fa-spin fa-3x-fa-fw';
  }

  if (testSuccess.value) {
    return 'fa fa-check-circle';
  }

  if (testFailed.value) {
    return 'fa fa-times-circle';
  }

  return 'fa fa-question-circle';
});

function testConnection() {
  isTesting.value = true;
  SettingsService.testConnection(values).then((response) => {
    if (response) {
      // successToast('Connection Status', 'Email Connection Successful!');
      testSuccess.value = true;
      testFailed.value = false;
    } else {
      // errorToast('Connection Status', 'Email Connection Failed');
      testSuccess.value = false;
      testFailed.value = true;
    }
  }).catch(() => {
    // errorToast('Failure', 'Failed to Test the Email Connection');
  }).finally(() => {
    isTesting.value = false;
  });
}

function saveEmailSettings() {
  isSaving.value = true;
  if (authEnabled.value === false || authEnabled.value === 'false') {
    username.value = '';
    password.value = '';
  }
  SettingsService.saveEmailSettings(values).then((result) => {
    if (result) {
      if (result.success) {
        // successToast('Saved', 'Email Connection Successful!');
      } else {
        connectionError.value = result.explanation;
      }
    }
  }).catch(() => {
    // errorToast('Failure', 'Failed to Save the Connection Settings!');
  }).finally(() => {
    isSaving.value = false;
  });
}
</script>

<template>
  <div data-cy="emailConnectionSettings">
    <SkillsTextInput name="publicUrl" label="Public URL" is-required />
    <SkillsTextInput name="fromEmail" label="From Email" is-required />
    <SkillsTextInput name="host" label="Host" is-required />
    <SkillsNumberInput class="w-full" label="Port" :min="0" name="port" is-required :useGrouping="false" />
    <SkillsTextInput name="protocol" label="Protocol" is-required />

    <div>
      <ToggleSwitch v-model="tlsEnabled" class="mr-2" data-cy="tlsSwitch" inputId="tlsSwitch" aria-labelledby="tlsSwitchLabel"/> <label for="tlsSwitch" id="tlsSwitchLabel">TLS {{ tlsEnabled ? 'Enabled' : 'Disabled'}}</label>
    </div>
    <div class="mt-2">
      <ToggleSwitch v-model="authEnabled" class="mr-2" data-cy="authSwitch" inputId="authSwitch" aria-labelledby="authLabel"/> <label for="authSwitch" id="authLabel">Authentication {{ authEnabled ? 'Enabled' : 'Disabled'}}</label>
      <Card v-if="authEnabled" class="mt-2">
        <template #header>
          <SkillsCardHeader title="Authentication Info"></SkillsCardHeader>
        </template>
        <template #content>
          <SkillsTextInput name="username" label="Username" is-required />
          <SkillsTextInput name="password" label="Password" is-required />
        </template>
      </Card>
    </div>

    <Message v-if="!isTesting && (testFailed || testSuccess || connectionError)" :severity="(testFailed || connectionError ? 'error' : 'success')">
      <span v-if="testFailed">Email Connection Failed</span>
      <span v-if="testSuccess">Email Connection Successful</span>
      <span v-if="connectionError">{{ connectionError }}</span>
    </Message>

    <div class="flex gap-2 mt-6">
      <SkillsButton v-on:click="testConnection" :disabled="!meta.valid || isTesting || isSaving"
              data-cy="emailSettingsTest" label="Test" :icon="testButtonClass" aria-roledescription="test email server settings button">
      </SkillsButton>
      <SkillsButton v-on:click="saveEmailSettings" :disabled="!meta.valid || !meta.dirty || isSaving || isTesting"
                    data-cy="emailSettingsSave" label="Save" :icon="isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right'">
      </SkillsButton>
    </div>
  </div>
</template>

<style scoped>

</style>