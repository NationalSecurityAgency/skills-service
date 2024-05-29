<script setup>
import { ref, onMounted } from 'vue';
import { object, string } from 'yup';
import {useForm} from "vee-validate";
import TabPanel from 'primevue/tabpanel';
import TabView from 'primevue/tabview';
import SettingsService from "@/components/settings/SettingsService.js";

const checkDependentValue = (value, dependency, ctx) => {
  const dependencyLength = dependency.length;
  if(value.length > 0) {
    return dependencyLength > 0 ? true : ctx.createError({ message: `${dependency.label} is also required` });
  } else if(dependencyLength > 0 ){
    return ctx.createError({ message: `${value.label} is required` });
  } else {
    return true;
  }
}

const schema = object({
  htmlHeader: string()
      .max(3000)
      .noScript()
      .label('HTML Header')
      .test({
        name: 'require-other-field',
        test(value, ctx) {
          return checkDependentValue(
              {label: 'HTML Header', length: value?.length},
              {label: 'Plaintext Header', length: ctx.parent.plainTextHeader?.length},
              ctx
          );
        }
      }),
  plainTextHeader: string()
      .max(3000)
      .noScript()
      .label('Plaintext Header')
      .test({
        name: 'require-other-field',
        test(value, ctx) {
          return checkDependentValue(
              {label: 'Plaintext Header', length: value?.length},
              {label: 'HTML Header', length: ctx.parent.htmlHeader?.length},
              ctx
          );
        }
      }),
  htmlFooter: string()
      .max(3000)
      .noScript()
      .label('HTML Footer')
      .test({
        name: 'require-other-field',
        test(value, ctx) {
          return checkDependentValue(
              {label: 'HTML Footer', length: value?.length},
              {label: 'Plaintext Footer', length: ctx.parent.plainTextFooter?.length},
              ctx
          );
        }
      }),
  plainTextFooter: string()
      .max(3000)
      .noScript()
      .label('Plaintext Footer')
      .test({
        name: 'require-other-field',
        test(value, ctx) {
          return checkDependentValue(
              {label: 'Plaintext Footer', length: value?.length},
              {label: 'HTML Footer', length: ctx.parent.htmlFooter?.length},
              ctx
          );
        }
      }),
});

const { defineField, meta } = useForm({
  validationSchema: schema,
  initialValues: {
    htmlHeader: '',
    htmlFooter: '',
    plainTextHeader: '',
    plainTextFooter: '',
  }
});

const [htmlHeader] = defineField('htmlHeader');
const [htmlFooter] = defineField('htmlFooter');
const [plainTextHeader] = defineField('plainTextHeader');
const [plainTextFooter] = defineField('plainTextFooter');

const settingGroup = 'GLOBAL.EMAIL';
const isSaving = ref(false);
const htmlHeaderText = ref('HTML Header');
const htmlFooterText = ref('HTML Footer');
const plainTextHeaderText = ref('Plaintext Header');
const plainTextFooterText = ref('Plaintext Footer');
const saveMessage = ref('');

onMounted(() => {
  loadEmailSettings()
});
const saveTemplateSettings = () => {
  isSaving.value = true;
  saveMessage.value = '';
  const settings = convertToSettings();
  SettingsService.saveGlobalSettings(settings).then((result) => {
    if (result) {
      if (result.success) {
        saveMessage.value = 'Email Template Settings Saved!';
      }
    }
  })
  .catch(() => {
    saveMessage.value = 'Failed to Save the Email Template Settings!';
  })
  .finally(() => {
    isSaving.value = false;
  });
};

const convertToSettings = () => {
  return [{
    settingGroup,
    setting: 'email.htmlHeader',
    value: htmlHeader.value,
  },
  {
    settingGroup,
    setting: 'email.htmlFooter',
    value: htmlFooter.value,
  },
  {
    settingGroup,
    setting: 'email.plaintextHeader',
    value: plainTextHeader.value,
  },
  {
    settingGroup,
    setting: 'email.plaintextFooter',
    value: plainTextFooter.value,
  }];
};

const convertFromSettings = (settings) => {
  let htmlHeaderSaved = settings.find((setting) => setting.setting === 'email.htmlHeader')?.value;
  let htmlFooterSaved = settings.find((setting) => setting.setting === 'email.htmlFooter')?.value;
  let plainTextHeaderSaved = settings.find((setting) => setting.setting === 'email.plaintextHeader')?.value;
  let plainTextFooterSaved = settings.find((setting) => setting.setting === 'email.plaintextFooter')?.value;

  htmlHeader.value = htmlHeaderSaved ? htmlHeaderSaved : htmlHeader.value;
  htmlFooter.value = htmlFooterSaved ? htmlFooterSaved : htmlFooter.value;
  plainTextHeader.value = plainTextHeaderSaved ? plainTextHeaderSaved : plainTextHeader.value;
  plainTextFooter.value = plainTextFooterSaved ? plainTextFooterSaved : plainTextFooter.value;
};

const loadEmailSettings = () => {
  SettingsService.getGlobalSettings(settingGroup).then((response) => {
    convertFromSettings(response);
  });
};
</script>

<template>
  <div data-cy="emailTemplateSettings">

    <SkillsTextarea name="htmlHeader" label="HTML Header" />
    <SkillsTextarea name="plainTextHeader" label="Plain Text Header" />

    <SkillsTextarea name="htmlFooter" label="HTML Footer" />
    <SkillsTextarea name="plainTextFooter" label="Plain Text Footer" />

    <Message v-if="saveMessage" :sticky="false" :life="10000">{{saveMessage}}</Message>

    <SkillsButton v-on:click="saveTemplateSettings" :disabled="!meta.valid || !meta.dirty || isSaving" data-cy="emailTemplateSettingsSave"
                  label="Save" :icon="isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right'" >
    </SkillsButton>
  </div>
</template>

<style scoped>

</style>