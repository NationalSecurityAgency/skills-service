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
import { computed, onMounted, nextTick, ref } from 'vue';
import { useSkillsState } from '@/stores/UseSkillsState.js';
import { useRoute } from 'vue-router';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'
import * as yup from 'yup';
import { useForm } from 'vee-validate';
import dayjs from '@/common-components/DayJsCustomizer'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import SkillsOverlay from '@/components/utils/SkillsOverlay.vue';
import ExpirationService from '@/components/expiration/ExpirationService.js';
import SkillsNumberInput from '@/components/utils/inputForm/SkillsNumberInput.vue';
import SkillsDropDown from '@/components/utils/inputForm/SkillsDropDown.vue';
import SkillsRadioButtonInput from '@/components/utils/inputForm/SkillsRadioButtonInput.vue';
import SkillsButton from '@/components/utils/inputForm/SkillsButton.vue';
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import EmailNotEnabledWarning from '@/components/utils/EmailNotEnabledWarning.vue'

const skillsState = useSkillsState();
const announcer = useSkillsAnnouncer()
const route = useRoute()
const responsive = useResponsiveBreakpoints()
const appConfig = useAppConfig()
const appInfo = useAppInfoState()

const NEVER = 'NEVER';
const YEARLY = 'YEARLY';
const MONTHLY = 'MONTHLY';
const DAILY = 'DAILY';
const FIRST_DAY_OF_MONTH = 'FIRST_DAY_OF_MONTH';
const LAST_DAY_OF_MONTH = 'LAST_DAY_OF_MONTH';
const SET_DAY_OF_MONTH = 'SET_DAY_OF_MONTH';

const monthlyDayCategories = ref([
  { name: 'First', key: FIRST_DAY_OF_MONTH },
  { name: 'Last', key: LAST_DAY_OF_MONTH },
  { name: 'Other', key: SET_DAY_OF_MONTH },
])
const yearlyYears = ref(1);
const yearlyMonth = ref(0);
const yearlyDayOfMonth = ref(1);
const monthlyMonths = ref(1);
const monthlyDay = ref(1);
const monthlyDayOption = ref(FIRST_DAY_OF_MONTH);
const dailyDays = ref(90);
const expirationType = ref(NEVER);
const emailNotificationsEnabled = ref(false);
const loading = ref(true);
const saving = ref(false);
const showSavedMsg = ref(false);
const loadedSettings = ref({});
const monthsOptions = computed(() => {
  return dayjs.months().map((m, index) => ({ value: index, text: m }));
});
const dayOptions = computed(() => {
  const daysInSelectedMonth = dayjs().month(yearlyMonth.value).daysInMonth();
  return Array.from({ length: daysInSelectedMonth }, (_, index) => ({ value: index + 1, text: `${index + 1}` }));
});
const isImported = computed(() => {
  return skillsState.skill && skillsState.skill.copiedFromProjectId && skillsState.skill.copiedFromProjectId.length > 0 && !skillsState.skill.reusedSkill;
});
const isReused = computed(() => {
  return skillsState.skill && skillsState.skill.reusedSkill;
});
const isReadOnly = computed(() => {
  return isReused.value || isImported.value;
});

const nextMonthlyExpirationDate = computed(() => {
  let nextExpirationDate = null
  if (expirationType.value === MONTHLY) {
    // calculate next expiration date
    const now = dayjs().utc();
    const currentDayOfMonth = now.date();
    let incrementMonthBy = monthlyMonths.value
    if (monthlyDayOption.value === LAST_DAY_OF_MONTH || (monthlyDayOption.value === SET_DAY_OF_MONTH && currentDayOfMonth <= monthlyDay.value)) {
      incrementMonthBy -= 1
    }
    nextExpirationDate = dayjs(new Date(now.year(), now.month(), now.day())).date(1).add(incrementMonthBy, 'month');
    if (monthlyDayOption.value === FIRST_DAY_OF_MONTH) {
      nextExpirationDate = nextExpirationDate.date(1)
    } else if (monthlyDayOption.value === LAST_DAY_OF_MONTH) {
      nextExpirationDate = nextExpirationDate.endOf('month')
    } else if (monthlyDayOption.value === SET_DAY_OF_MONTH) {
      const nextExpirationDateDay = monthlyDay.value <= nextExpirationDate.daysInMonth() ? monthlyDay.value : nextExpirationDate.endOf('month').date();
      nextExpirationDate = nextExpirationDate.date(nextExpirationDateDay)
    }
  }
  return nextExpirationDate
})

const nextYearlyExpirationDate = computed(() => {
  let nextExpirationDate = null
  if (expirationType.value === YEARLY) {
    // calculate next expiration date
    const now = dayjs()
    const currentMonth = now.month()
    const currentDayOfMonth = now.date()
    let incrementYearBy = yearlyYears.value
    if (currentMonth < yearlyMonth.value || (currentMonth === yearlyMonth.value && currentDayOfMonth < yearlyDayOfMonth.value)) {
      incrementYearBy -= 1;
    }
    nextExpirationDate = dayjs(new Date(now.year() + incrementYearBy, yearlyMonth.value, yearlyDayOfMonth.value))
  }
  return nextExpirationDate
})

onMounted(() => {
  const now = dayjs();
  yearlyMonth.value = now.month();
  yearlyDayOfMonth.value = now.date();
  loadSettings();
  // loadSkillInfo();
});
const loadSettings = () => {
  loading.value = true;
  ExpirationService.getExpirationSettings(route.params.projectId, route.params.skillId)
      .then((expirationSettings) => {
        expirationType.value = expirationSettings.expirationType;
        if (expirationSettings.expirationType === YEARLY) {
          yearlyYears.value = expirationSettings.every;
          const nextExpirationDate = dayjs(expirationSettings.nextExpirationDate);
          yearlyMonth.value = nextExpirationDate.month();
          yearlyDayOfMonth.value = nextExpirationDate.date();
        } else if (expirationSettings.expirationType === MONTHLY) {
          monthlyMonths.value = expirationSettings.every;
          if (expirationSettings.monthlyDay !== FIRST_DAY_OF_MONTH && expirationSettings.monthlyDay !== LAST_DAY_OF_MONTH) {
            monthlyDayOption.value = SET_DAY_OF_MONTH;
            expirationSettings.monthlyDay = isNaN(parseInt(expirationSettings.monthlyDay)) ? 1 : parseInt(expirationSettings.monthlyDay); // convert to integer, default to 1 if not a number
            monthlyDay.value =expirationSettings.monthlyDay; // in this case, monthly day is the actual day of the month
          } else {
            monthlyDayOption.value = expirationSettings.monthlyDay;
          }
        } else if (expirationSettings.expirationType === DAILY) {
          dailyDays.value = expirationSettings.every;
        }
        emailNotificationsEnabled.value = expirationSettings.emailNotificationsEnabled;
        updateLoadedSettings(expirationSettings);

      }).finally(() => {
    loading.value = false;
  });
}
const setFieldValues = () => {
  resetForm({
    values: {
      emailNotificationsEnabled: emailNotificationsEnabled.value,
      expirationType: expirationType.value,
      yearlyYears: yearlyYears.value,
      yearlyMonth: yearlyMonth.value,
      yearlyDayOfMonth: yearlyDayOfMonth.value,
      monthlyMonths: monthlyMonths.value,
      monthlyDayOption: monthlyDayOption.value,
      monthlyDay: monthlyDay.value,
      dailyDays: dailyDays.value
    }
  });
}
const updateLoadedSettings = (expirationSettings) => {
  loadedSettings.value.emailNotificationsEnabled = expirationSettings.emailNotificationsEnabled;
  loadedSettings.value.expirationType = expirationSettings.expirationType;
  loadedSettings.value.every = expirationSettings.every;
  loadedSettings.value.monthlyDay = expirationSettings.monthlyDay;
  loadedSettings.value.nextExpirationDate = expirationSettings.nextExpirationDate;
  setFieldValues()
}

const schema = yup.object().shape({
  'emailNotificationsEnabled' : yup.boolean(),
  'expirationType': yup.string(),
  'yearlyYears': yup.number()
      .when('expirationType', {
        is: YEARLY,
        then: (sch)  => sch
            .required()
            .min(1)
            .max(99)
            .label('Years')
            .typeError('Years must be a number between 1 and 1000'),
      }),
  'yearlyMonth': yup.number()
      .when('expirationType', {
        is: YEARLY,
        then: (sch)  => sch
            .required()
            .min(0)
            .max(11)
            .label('Month'),
      }),
  'yearlyDayOfMonth': yup.number()
      .when('expirationType', {
        is: YEARLY,
        then: (sch)  => sch
            .required()
            .min(1)
            .max(31)
            .label('Day')
            .typeError('Day be a number between 1 and 31'),
      }),
  'monthlyMonths': yup.number()
      .when('expirationType', {
        is: MONTHLY,
        then: (sch)  => sch
            .required()
            .min(1)
            .max(99)
            .label('Months')
            .typeError('Months must be a number between 1 and 1000'),
      }),
  'monthlyDayOption': yup.string()
      .when('expirationType', {
        is: MONTHLY,
        then: (sch)  => sch
            .required()
            .test('isValidMonthlyDayOption', 'Invalid Day of Month Option', (selected) => monthlyDayCategories.value.map((m) => m.key).includes(selected))
            .label('Day of Month Option'),
      }),
  'monthlyDay': yup.number()
      .when('expirationType', {
        is: (expirationType, monthlyDayOption) => expirationType === MONTHLY && monthlyDayOption === SET_DAY_OF_MONTH,
        then: (sch)  => sch
            .required()
            .min(1)
            .max(31)
            .label('Day')
            .typeError('Day be a number between 1 and 31'),
      }),
  'dailyDays': yup.number()
      .when('expirationType', {
        is: DAILY,
        then: (sch)  => sch
            .required()
            .min(1)
            .max(999)
            .label('Expiration Days')
            .typeError('Expiration Days must be a number between 1 and 1000'),
      }),
})

const { values, meta, handleSubmit, resetForm, setFieldValue, validate, errors } = useForm({ validationSchema: schema, })
const saveSettings = handleSubmit((values) => {
  saving.value = true;
  loading.value = true;
  const expirationSettings = {
    expirationType: expirationType.value,
    every: null,
    monthlyDay: null,
    nextExpirationDate: null,
    emailNotificationsEnabled: false,
  };
  if (expirationType.value !== NEVER) {
    const now = dayjs();
    const currentMonth = now.month();
    const currentDayOfMonth = now.date();
    expirationSettings.emailNotificationsEnabled = emailNotificationsEnabled.value;
    if (expirationType.value === YEARLY) {
      expirationSettings.every = yearlyYears.value;
      expirationSettings.nextExpirationDate = nextYearlyExpirationDate.value;
    } else if (expirationType.value === MONTHLY) {
      expirationSettings.every = monthlyMonths.value;
      expirationSettings.monthlyDay = monthlyDayOption.value === SET_DAY_OF_MONTH ? monthlyDay.value : monthlyDayOption.value;
      expirationSettings.nextExpirationDate = nextMonthlyExpirationDate.value;
    } else if (expirationType.value === DAILY) {
      expirationSettings.every = dailyDays.value;
      // any user achievement achievedOn before this date
      // expirationSettings.nextExpirationDate = dayjs(now).subtract(dailyDays.value, 'day');
    }
    ExpirationService.saveExpirationSettings(route.params.projectId, route.params.skillId, expirationSettings)
        .then(() => {
          updateLoadedSettings(expirationSettings);
          showSavedMsg.value = true;
          setTimeout(() => {
            showSavedMsg.value = false;
          }, 3500);
          nextTick(() => announcer.polite('Expiration settings were saved'));
        })
        .finally(() => {
          loading.value = false;
          saving.value = false;
        });
  } else {
    // expirationType changed to NEVER so delete existing settings
    ExpirationService.deleteExpirationSettings(route.params.projectId, route.params.skillId, expirationSettings)
        .then(() => {
          updateLoadedSettings(expirationSettings);
          showSavedMsg.value = true;
          setTimeout(() => {
            showSavedMsg.value = false;
          }, 3500);
          nextTick(() => announcer.polite('Expiration settings were saved'));
        })
        .finally(() => {
          loading.value = false;
          saving.value = false;
        });
  }
});

const resetYearlyDayOfMonth = () => {
  yearlyDayOfMonth.value=1;
  setFieldValue('yearlyDayOfMonth', 1);
}

</script>

<template>
  <div>
    <SubPageHeader title="Configure Expiration" :helpDocsUrl="`${appConfig.docsHost}/dashboard/user-guide/skills.html#expire-points-and-achievements`" />
    <SkillsOverlay :show="!!loading || !!skillsState.loadingSkill || !!saving">
      <Card>
        <template #content>
          <Message v-if="isReadOnly" severity="info" icon="fas fa-exclamation-triangle" data-cy="readOnlyAlert" :closable="false">
            Expiration attributes of
            <span v-if="isImported"><Tag severity="infosuccess"><i class="fas fa-book mr-1" aria-hidden="true"/> Imported</Tag></span>
            <span v-if="isReused"><Tag severity="success"><i class="fas fa-recycle mr-1" aria-hidden="true"/> Reused</Tag></span>
            skills are read-only.
          </Message>
          <BlockUI :blocked="isReadOnly">
            <div class="flex flex-col" data-cy="expirationTypeSelector">
              <div class="rounded-border p-4" :class="{ 'surface-100' : expirationType === NEVER}">
                <div class="flex items-center justify-start">
                  <div class="flex flex-wrap">
                    <div class="flex items-center">
                      <SkillsRadioButtonInput v-model="expirationType"
                                              :disabled="isReadOnly"
                                   inputId="expirationTypeNone"
                                   name="expirationType"
                                   data-cy="expirationNeverRadio"
                                   :value="NEVER" />
                      <label for="expirationTypeNone" class="ml-2 font-bold">Never</label>
                    </div>
                  </div>
                </div>
             </div>

             <Divider />

              <Fieldset legend="Expiration Configuration" :pt="{ legend: { style: 'display: none' } }">
                <div v-if="appInfo.emailEnabled" class="mb-2 mt-4 mx-2 flex align-items-center">
                  <SkillsCheckboxInput
                    :binary="true"
                    v-model="emailNotificationsEnabled"
                    :disabled="expirationType === NEVER || isReadOnly"
                    inputId="emailNotificationsEnabled"
                    name="emailNotificationsEnabled"
                    data-cy="emailNotificationsEnabledCheckbox"
                    :value="true" />
                  <label for="emailNotificationsEnabled" class="ml-2 mb-0" :class="{ 'text-color-secondary' : expirationType !== NEVER || isReadOnly }">Email Notifications Enabled</label>
                </div>
                <email-not-enabled-warning/>

                <Divider />

                <div class="rounded-border p-4 mb-4" :class="{ 'surface-100' : expirationType === YEARLY}">
                  <div class="flex items-center justify-start">
                    <div class="flex flex-wrap">
                      <div class="flex items-center">
                        <SkillsRadioButtonInput v-model="expirationType"
                                                :disabled="isReadOnly"
                                     inputId="yearlyRadio"
                                     name="expirationType"
                                     data-cy="yearlyRadio"
                                     :value="YEARLY" />
                        <label for="yearlyRadio" class="ml-2 font-bold">Yearly</label>
                      </div>
                    </div>
                  </div>

                  <div class="flex flex-wrap md:flex-nowrap ml-8 gap-2" :class="{ 'text-color-secondary' : expirationType !== YEARLY}" data-cy="yearlyFormGroup">
                    <div class="flex flex-col md:flex-row gap-2 items-baseline gap-2" :class="{'w-full': responsive.md.value }">
                      <label for="inputyearlyYears" class="">Skills will expire every</label>
                      <SkillsNumberInput
                          id="yearlyYears-sb"
                          data-cy="yearlyYears-sb"
                          v-model="yearlyYears"
                          :disabled="expirationType !== 'YEARLY' || isReadOnly"
                          :class="{'w-full': responsive.md.value }"
                          name="yearlyYears"
                          inputClass="w-24"
                          inputId="minmax-buttons"
                          :suffix="` year${yearlyYears > 1 ? 's' : ''}`"
                          :min="0" :max="99"/>
                      <!--                  <span class="ml-2">year{{yearlyYears > 1 ? 's' : ''}} on:</span>-->
                    </div>
                    <div class="flex items-baseline flex-col md:flex-row gap-2" :class="{'w-full': responsive.md.value }">
                      <span class="">on:</span>
                      <SkillsDropDown :options="monthsOptions"
                                      v-model="yearlyMonth"
                                      :disabled="expirationType !== YEARLY || isReadOnly"
                                      :class="{'w-full': responsive.md.value }"
                                      name="yearlyMonth"
                                      optionLabel="text"
                                      optionValue="value"
                                      @change="resetYearlyDayOfMonth"
                                      aria-label="Month of year"
                                      data-cy="yearlyMonth"/>
                      <SkillsDropDown v-model="yearlyDayOfMonth"
                                      :options="dayOptions"
                                      :disabled="expirationType !== YEARLY || isReadOnly"
                                      :class="{'w-full': responsive.md.value }"
                                      aria-label="Day of month"
                                      name="yearlyDayOfMonth"
                                      optionLabel="text"
                                      optionValue="value"
                                      data-cy="yearlyDayOfMonth"/>
                    </div>
                  </div>
                  <div v-if="nextYearlyExpirationDate" data-cy="nextYearlyExpirationDate"class="ml-8 mt-2">
                    Next expiration date: <Tag>{{ nextYearlyExpirationDate.format('YYYY-MM-DD') }}</Tag>
                  </div>
                </div>

                <div class="rounded-border p-4" :class="{ 'surface-100' : expirationType === MONTHLY}" data-cy="monthlyFormGroup">
                  <div class="flex items-center justify-start">
                    <div class="flex flex-wrap">
                      <div class="flex items-center">
                        <SkillsRadioButtonInput v-model="expirationType"
                                     inputId="monthlyRadio"
                                     name="expirationType"
                                     data-cy="monthlyRadio"
                                     :disabled="isReadOnly"
                                     :value="MONTHLY" />
                        <label for="monthlyRadio" class="ml-2 font-bold">Monthly</label>
                      </div>
                    </div>
                  </div>

                  <div class="flex flex-wrap md:flex-nowrap ml-8 gap-2" :class="{ 'text-color-secondary' : expirationType !== MONTHLY}">
                    <div class="flex flex-col md:flex-row items-baseline gap-2" :class="{'w-full': responsive.md.value }">
                      <label for="inputmonthlyMonths" class="">Skills will expire every</label>
                      <SkillsNumberInput
                          id="monthlyMonths-sb"
                          data-cy="monthlyMonths-sb"
                          v-model="monthlyMonths"
                          :class="{'w-full': responsive.md.value }"
                          :disabled="expirationType !== MONTHLY || isReadOnly"
                          name="monthlyMonths"
                          inputClass="w-24"
                          inputId="minmax-buttons"
                          :suffix="` month${monthlyMonths > 1 ? 's' : ''}`"
                          :min="0" :max="99"/>
                      <!--                  <span class="ml-2">year{{monthlyMonths > 1 ? 's' : ''}} on:</span>-->
                    </div>
                    <div class="flex items-baseline flex-col md:flex-row gap-2" :class="{'w-full': responsive.md.value }">
                      <div class="flex gap-2">
                        <span class="">on:</span>
                        <div class="flex flex-wrap flex-col md:flex-row gap-4" data-cy="monthlyDayOption">
                          <div v-for="category in monthlyDayCategories" :key="category.key" class="flex items-center">
                            <SkillsRadioButtonInput v-model="monthlyDayOption" :inputId="category.key"
                                                    :disabled="expirationType !== MONTHLY || isReadOnly"
                                                    name="monthlyDayOption" :value="category.key"/>
                            <label :for="category.key" class="ml-2">{{ category.name }}</label>
                          </div>
                        </div>
                      </div>
                      <SkillsDropDown v-model="monthlyDay"
                                      :options="dayOptions"
                                      :disabled="expirationType !== MONTHLY || monthlyDayOption !== 'SET_DAY_OF_MONTH'"
                                      :class="{'w-full': responsive.md.value }"
                                      aria-label="Set day of month"
                                      name="monthlyDay"
                                      optionLabel="text"
                                      optionValue="value"
                                      data-cy="monthlyDay"/>
                    </div>
                  </div>
                  <div v-if="nextMonthlyExpirationDate" data-cy="nextMonthlyExpirationDate"class="ml-8 mt-2">
                    Next expiration date: <Tag>{{ nextMonthlyExpirationDate.format('YYYY-MM-DD') }}</Tag>
                  </div>
                </div>

                <Divider/>

                <div class="rounded-border p-4" :class="{ 'surface-100' : expirationType === DAILY}" data-cy="dailyFormGroup">
                  <div class="flex items-center justify-start">
                    <div class="flex flex-wrap">
                      <div class="flex items-center mb-2 md:mb-0">
                        <SkillsRadioButtonInput v-model="expirationType"
                                     inputId="dailyRadio"
                                     name="expirationType"
                                     data-cy="dailyRadio"
                                     :disabled="isReadOnly"
                                     :value="DAILY" />
                        <label for="dailyRadio" class="ml-2 font-bold">Daily with ability to retain</label>
                      </div>
                    </div>
                  </div>

                  <div class="flex flex-wrap md:flex-nowrap ml-8 gap-2" :class="{ 'text-color-secondary' : expirationType !== DAILY}">
                    <div class="flex items-baseline flex-col md:flex-row gap-2" :class="{'w-full': responsive.md.value }">
                      <label for="dailyDays-sb" class="">Achievement will expire after</label>
                      <SkillsNumberInput
                          id="dailyDays-sb"
                          data-cy="dailyDays-sb"
                          v-model="dailyDays"
                          :disabled="expirationType !== DAILY || isReadOnly"
                          :class="{'w-full': responsive.md.value }"
                          :aria-label="`Skills will expire every ${dailyDays} days after user earns an achievement`"
                          name="dailyDays"
                          inputClass="w-24"
                          inputId="minmax-buttons"
                          :suffix="` day${dailyDays > 1 ? 's' : ''}`"
                          :min="0" :max="999"/>
                      <span class="">of inactivity</span>
                    </div>
                  </div>
                </div>
              </Fieldset>

              <Divider />

              <div class="flex flex-row">
                <div class="flex gap-1">
                  <SkillsButton variant="outline-success"
                                label="Save"
                                icon="fas fa-arrow-circle-right"
                                @click="saveSettings"
                                :disabled="!meta.valid || !meta.dirty || isReadOnly"
                                aria-label="Save Settings"
                                data-cy="saveSettingsBtn">
                  </SkillsButton>

                  <InlineMessage v-if="meta.dirty"
                                 severity="warn"
                                 class="ml-2"
                                 data-cy="unsavedChangesAlert"
                                 aria-label="Settings have been changed, do not forget to save">
                    Unsaved Changes
                  </InlineMessage>
                  <InlineMessage v-if="!meta.dirty && showSavedMsg"
                                 severity="success"
                                 class="ml-2"
                                 data-cy="settingsSavedAlert">
                    Settings Updated!
                  </InlineMessage>
                </div>
              </div>
            </div>
          </BlockUI>
        </template>
      </Card>
    </SkillsOverlay>
  </div>
</template>

<style scoped></style>
