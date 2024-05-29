<script setup>
import { ref, computed, onMounted } from 'vue';
import { object, string, boolean, number } from 'yup';
import {useForm} from "vee-validate";
import SettingsService from '@/components/settings/SettingsService.js';

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

const { defineField, errors, meta } = useForm({
  validationSchema: schema,
  initialValues: {
    host: 'localhost',
    port: 25,
    protocol: 'smtp',
    username: '',
    password: '',
    authEnabled: false,
    tlsEnabled: false,
    publicUrl: '',
    fromEmail: 'no_reply@skilltree',
  }
})

const [publicUrl] = defineField('publicUrl');
const [fromEmail] = defineField('fromEmail');
const [host] = defineField('host');
const [port] = defineField('port');
const [protocol] = defineField('protocol');
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

onMounted(() => {
  loadEmailSettings();
});

const getEmailInfoAsObject = () => {
  return {
    publicUrl: publicUrl.value,
    fromEmail: fromEmail.value,
    host: host.value,
    port: port.value,
    protocol: protocol.value,
    tlsEnabled: tlsEnabled.value,
    authEnabled: authEnabled.value,
    username: username.value,
    password: password.value
  };
};
function testConnection() {
  isTesting.value = true;
  SettingsService.testConnection(getEmailInfoAsObject()).then((response) => {
    if (response) {
      // successToast('Connection Status', 'Email Connection Successful!');
      testSuccess.value = true;
      testFailed.value = false;
    } else {
      // errorToast('Connection Status', 'Email Connection Failed');
      testSuccess.value = false;
      testFailed.value = true;
    }
  })
      .catch(() => {
        // errorToast('Failure', 'Failed to Test the Email Connection');
      })
      .finally(() => {
        isTesting.value = false;
      });
}

function saveEmailSettings() {
  isSaving.value = true;
  if (authEnabled.value === false || authEnabled.value === 'false') {
    username.value = '';
    password.value = '';
  }
  SettingsService.saveEmailSettings(getEmailInfoAsObject()).then((result) => {
    if (result) {
      if (result.success) {
        // successToast('Saved', 'Email Connection Successful!');
      } else {
        connectionError.value = result.explanation;
      }
    }
  })
      .catch(() => {
        // errorToast('Failure', 'Failed to Save the Connection Settings!');
      })
      .finally(() => {
        isSaving.value = false;
      });
}

function loadEmailSettings() {
  SettingsService.loadEmailSettings().then((response) => {
    publicUrl.value = response.publicUrl;
    fromEmail.value = response.fromEmail;
    host.value = response.host;
    port.value = response.port;
    protocol.value = response.protocol;
    tlsEnabled.value = response.tlsEnabled;
    authEnabled.value = response.authEnabled;
    username.value = response.username;
    password.value = response.password;
  });
}

function missingRequiredValues() {
  return !isAuthValid() || !host.value || !port.value || !protocol.value || !publicUrl.value || !fromEmail.value;
}

function isAuthValid() {
  return !authEnabled.value || (username.value && password.value);
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
      <InputSwitch v-model="tlsEnabled" class="mr-2" data-cy="tlsSwitch" aria-labelledby="tlsSwitchLabel"/> <label id="tlsSwitchLabel">TLS {{ tlsEnabled ? 'Enabled' : 'Disabled'}}</label>
    </div>
    <div class="mt-2">
      <InputSwitch v-model="authEnabled" class="mr-2" data-cy="authSwitch" aria-labelledby="authSwitch"/> <label id="authSwitch">Authentication {{ authEnabled ? 'Enabled' : 'Disabled'}}</label>
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

    <div class="flex gap-2 mt-4">
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