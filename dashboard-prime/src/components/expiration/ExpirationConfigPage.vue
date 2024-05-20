<script setup>
import { computed, onMounted, nextTick, ref } from 'vue';
import { useSkillsState } from '@/stores/UseSkillsState.js';
import { useRoute } from 'vue-router';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js';
import * as yup from 'yup';
import { useForm } from 'vee-validate';
import dayjs from '@/common-components/DayJsCustomizer'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import SkillsOverlay from '@/components/utils/SkillsOverlay.vue';
import ExpirationService from '@/components/expiration/ExpirationService.js';
import SkillsNumberInput from '@/components/utils/inputForm/SkillsNumberInput.vue';
import SkillsDropDown from '@/components/utils/inputForm/SkillsDropDown.vue';
import SkillsRadioButtonInput from '@/components/utils/inputForm/SkillsRadioButtonInput.vue';

const skillsState = useSkillsState();
const announcer = useSkillsAnnouncer()
const route = useRoute()

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
const yearlyMonth = ref(1);
const yearlyDayOfMonth = ref(1);
const monthlyMonths = ref(1);
const monthlyDay = ref(1);
const monthlyDayOption = ref(FIRST_DAY_OF_MONTH);
const dailyDays = ref(90);
const expirationType = ref(NEVER);
const loading = ref(true);
const saving = ref(false);
const showSavedMsg = ref(false);
const loadedSettings = ref({});
const overallErrMsg = ref(null);

const isDirty = computed(()  => {
  if (loading.value) {
    return false;
  }
  if (loadedSettings.value.expirationType !== expirationType.value) {
    return true;
  }
  const nextExpirationDate = dayjs(loadedSettings.value.nextExpirationDate);
  if (expirationType.value === YEARLY) {
    return loadedSettings.value.every !== yearlyYears.value
        || nextExpirationDate.month() !== yearlyMonth.value
        || nextExpirationDate.date() !== yearlyDayOfMonth.value;
  }
  if (expirationType.value === MONTHLY) {
    const loadedMonthlyDay = loadedSettings.value.monthlyDay;
    return loadedSettings.value.every !== monthlyMonths.value
        || (monthlyDayOption.value === SET_DAY_OF_MONTH && loadedMonthlyDay !== monthlyDay.value)
        || ((monthlyDayOption.value === FIRST_DAY_OF_MONTH || monthlyDayOption.value === LAST_DAY_OF_MONTH) && loadedMonthlyDay !== monthlyDayOption.value);
  }
  if (expirationType.value === DAILY) {
    return loadedSettings.value.every !== dailyDays.value;
  }
  return false;
});
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
            monthlyDay.value = expirationSettings.monthlyDay; // in this case, monthly day is the actual day of the month
          } else {
            monthlyDayOption.value = expirationSettings.monthlyDay;
          }
        } else if (expirationSettings.expirationType === DAILY) {
          dailyDays.value = expirationSettings.every;
        }
        updateLoadedSettings(expirationSettings);

      }).finally(() => {
    loading.value = false;
  });
}
const setFieldValues = () => {
  resetForm({
    values: {
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
  loadedSettings.value.expirationType = expirationSettings.expirationType;
  loadedSettings.value.every = expirationSettings.every;
  loadedSettings.value.monthlyDay = expirationSettings.monthlyDay;
  loadedSettings.value.nextExpirationDate = expirationSettings.nextExpirationDate;
  setFieldValues()
}

const schema = yup.object().shape({
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
            .min(1)
            .max(12)
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

const { values, meta, handleSubmit, resetForm, validate, errors } = useForm({ validationSchema: schema, })
const saveSettings = handleSubmit((values) => {
  saving.value = true;
  loading.value = true;
  const expirationSettings = {
    expirationType: expirationType.value,
    every: null,
    monthlyDay: null,
    nextExpirationDate: null,
  };
  if (expirationType.value !== NEVER) {
    const now = dayjs();
    const currentMonth = now.month();
    const currentDayOfMonth = now.date();
    if (expirationType.value === YEARLY) {
      expirationSettings.every = yearlyYears.value;

      // calculate next expiration date
      let incrementYearBy = yearlyYears.value;
      if (currentMonth < yearlyMonth.value || (currentMonth === yearlyMonth.value && currentDayOfMonth <= yearlyDayOfMonth.value)) {
        incrementYearBy -= 1;
      }
      expirationSettings.nextExpirationDate = dayjs(new Date(now.year() + incrementYearBy, yearlyMonth.value, yearlyDayOfMonth.value));
    } else if (expirationType.value === MONTHLY) {
      expirationSettings.every = monthlyMonths.value;
      expirationSettings.monthlyDay = monthlyDayOption.value;

      // calculate next expiration date
      let incrementMonthBy = monthlyMonths.value;
      if (monthlyDayOption.value === LAST_DAY_OF_MONTH || (monthlyDayOption.value === SET_DAY_OF_MONTH && currentDayOfMonth <= monthlyDay.value)) {
        incrementMonthBy -= 1;
      }
      const nextExpirationDate = dayjs(new Date(now.year(), now.month(), now.day())).date(1).add(incrementMonthBy, 'month');
      if (monthlyDayOption.value === FIRST_DAY_OF_MONTH) {
        expirationSettings.nextExpirationDate = nextExpirationDate.date(1);
      } else if (monthlyDayOption.value === LAST_DAY_OF_MONTH) {
        expirationSettings.nextExpirationDate = nextExpirationDate.endOf('month');
      } else if (monthlyDayOption.value === SET_DAY_OF_MONTH) {
        const nextExpirationDateDay = monthlyDay.value <= nextExpirationDate.daysInMonth() ? monthlyDay.value : nextExpirationDate.endOf('month').date();
        expirationSettings.nextExpirationDate = nextExpirationDate.date(nextExpirationDateDay);
        expirationSettings.monthlyDay = monthlyDay.value;
      }
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
</script>

<template>
  <div>
    <SubPageHeader title="Configure Expiration" />
    <SkillsOverlay :show="loading || skillsState.loadingSkill">
<!--      :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }"-->
      <Card v-if="saving || (!loading && !skillsState.loadingSkill)">
        <template #content>
          <Message v-if="isReadOnly" severity="info" icon="fas fa-exclamation-triangle" data-cy="readOnlyAlert" :closable="false">
            Expiration attributes of
            <span v-if="isImported"><Tag severity="infosuccess"><i class="fas fa-book mr-1" aria-hidden="true"/> Imported</Tag></span>
            <span v-if="isReused"><Tag severity="success"><i class="fas fa-recycle mr-1" aria-hidden="true"/> Reused</Tag></span>
            skills are read-only.
          </Message>
          <div class="flex flex-column" data-cy="expirationTypeSelector">

            <div class="border-round p-3" :class="{ 'bg-gray-100' : expirationType === NEVER}">
              <div class="flex align-items-center justify-content-start">
                <div class="flex flex-wrap">
                  <div class="flex align-items-center">
                    <SkillsRadioButtonInput v-model="expirationType"
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

            <div class="border-round p-3 mb-3" :class="{ 'bg-gray-100' : expirationType === YEARLY}">
              <div class="flex align-items-center justify-content-start">
                <div class="flex flex-wrap">
                  <div class="flex align-items-center">
                    <SkillsRadioButtonInput v-model="expirationType"
                                 inputId="yearlyRadio"
                                 name="expirationType"
                                 data-cy="yearlyRadio"
                                 :value="YEARLY" />
                    <label for="yearlyRadio" class="ml-2 font-bold">Yearly</label>
                  </div>
                </div>
              </div>

              <div class="flex flex-wrap md:flex-nowrap ml-5 gap-2" :class="{ 'text-color-secondary' : expirationType !== YEARLY}" data-cy="yearlyFormGroup">
                <div class="flex align-items-baseline gap-2">
                  <label for="inputyearlyYears" class="">Skills will expire every</label>
                  <SkillsNumberInput
                      id="yearlyYears-sb"
                      data-cy="yearlyYears-sb"
                      v-model="yearlyYears"
                      :disabled="expirationType !== 'YEARLY'"
                      name="yearlyYears"
                      inputClass="w-6rem"
                      inputId="minmax-buttons"
                      showButtons
                      :suffix="` year${yearlyYears > 1 ? 's' : ''}`"
                      :min="0" :max="99"/>
                  <!--                  <span class="ml-2">year{{yearlyYears > 1 ? 's' : ''}} on:</span>-->
                </div>
                <div class="flex align-items-baseline gap-2">
                  <span class="">on:</span>
                  <SkillsDropDown :options="monthsOptions"
                                  v-model="yearlyMonth"
                                  :disabled="expirationType !== YEARLY"
                                  name="yearlyMonth"
                                  optionLabel="text"
                                  optionValue="value"
                                  @change="yearlyDayOfMonth=1"
                                  aria-label="Month of year"
                                  data-cy="yearlyMonth"/>
                  <SkillsDropDown v-model="yearlyDayOfMonth"
                                  :options="dayOptions"
                                  :disabled="expirationType !== YEARLY"
                                  aria-label="Day of month"
                                  name="yearlyDayOfMonth"
                                  optionLabel="text"
                                  optionValue="value"
                                  data-cy="yearlyDayOfMonth"/>
                </div>
              </div>
            </div>

            <div class="border-round p-3" :class="{ 'bg-gray-100' : expirationType === MONTHLY}" data-cy="monthlyFormGroup">
              <div class="flex align-items-center justify-content-start">
                <div class="flex flex-wrap">
                  <div class="flex align-items-center">
                    <SkillsRadioButtonInput v-model="expirationType"
                                 inputId="monthlyRadio"
                                 name="expirationType"
                                 data-cy="monthlyRadio"
                                 :value="MONTHLY" />
                    <label for="monthlyRadio" class="ml-2 font-bold">Monthly</label>
                  </div>
                </div>
              </div>

              <div class="flex flex-wrap md:flex-nowrap ml-5 gap-2" :class="{ 'text-color-secondary' : expirationType !== MONTHLY}">
                <div class="flex align-items-baseline gap-2">
                  <label for="inputmonthlyMonths" class="">Skills will expire every</label>
                  <SkillsNumberInput
                      id="monthlyMonths-sb"
                      data-cy="monthlyMonths-sb"
                      v-model="monthlyMonths"
                      :disabled="expirationType !== MONTHLY"
                      name="monthlyMonths"
                      inputClass="w-6rem"
                      inputId="minmax-buttons"
                      showButtons
                      :suffix="` month${monthlyMonths > 1 ? 's' : ''}`"
                      :min="0" :max="99"/>
                  <!--                  <span class="ml-2">year{{monthlyMonths > 1 ? 's' : ''}} on:</span>-->
                </div>
                <div class="flex align-items-baseline gap-2">
                  <span class="">on:</span>
                  <div class="flex flex-wrap gap-3" data-cy="monthlyDayOption">
                    <div  v-for="category in monthlyDayCategories" :key="category.key" class="flex align-items-center">
                      <SkillsRadioButtonInput v-model="monthlyDayOption" :inputId="category.key" name="monthlyDayOption" :value="category.key" />
                      <label :for="category.key" class="ml-2">{{ category.name }}</label>
                    </div>
                  </div>
                  <SkillsDropDown v-model="monthlyDay"
                                  :options="dayOptions"
                                  :disabled="expirationType !== MONTHLY || monthlyDayOption !== 'SET_DAY_OF_MONTH'"
                                  aria-label="Set day of month"
                                  name="monthlyDay"
                                  optionLabel="text"
                                  optionValue="value"
                                  data-cy="monthlyDay"/>
                </div>
              </div>
            </div>

            <Divider/>

            <div class="border-round p-3" :class="{ 'bg-gray-100' : expirationType === DAILY}" data-cy="dailyFormGroup">
              <div class="flex align-items-center justify-content-start">
                <div class="flex flex-wrap">
                  <div class="flex align-items-center">
                    <SkillsRadioButtonInput v-model="expirationType"
                                 inputId="dailyRadio"
                                 name="expirationType"
                                 data-cy="dailyRadio"
                                 :value="DAILY" />
                    <label for="dailyRadio" class="ml-2 font-bold">Daily with ability to retain</label>
                  </div>
                </div>
              </div>

              <div class="flex flex-wrap md:flex-nowrap ml-5 gap-2" :class="{ 'text-color-secondary' : expirationType !== DAILY}">
                <div class="flex align-items-baseline gap-2">
                  <label for="dailyDays-sb" class="">Achievement will expire after</label>
                  <SkillsNumberInput
                      id="dailyDays-sb"
                      data-cy="dailyDays-sb"
                      v-model="dailyDays"
                      :disabled="expirationType !== DAILY"
                      :aria-label="`Skills will expire every ${dailyDays} days after user earns an achievement`"
                      name="dailyDays"
                      inputClass="w-6rem"
                      inputId="minmax-buttons"
                      showButtons
                      :suffix="` day${dailyDays > 1 ? 's' : ''}`"
                      :min="0" :max="999"/>
                  <span class="">of inactivity</span>
                </div>
              </div>
            </div>

            <Divider />

            <div class="flex flex-row">
              <div class="">
                <SkillsButton variant="outline-success"
                              label="Save"
                              icon="fas fa-arrow-circle-right"
                              @click="saveSettings"
                              :disabled="!meta.valid || !isDirty"
                              aria-label="Save Settings"
                              data-cy="saveSettingsBtn">
                </SkillsButton>

                <InlineMessage v-if="isDirty"
                               severity="warn"
                               class="ml-2"
                               data-cy="unsavedChangesAlert"
                               aria-label="Settings have been changed, do not forget to save">
                  Unsaved Changes
                </InlineMessage>
                <InlineMessage v-if="!isDirty && showSavedMsg"
                               severity="success"
                               class="ml-2"
                               data-cy="settingsSavedAlert">
                  Settings Updated!
                </InlineMessage>
              </div>
            </div>

          </div>

        </template>
      </Card>
    </SkillsOverlay>
  </div>
</template>

<style scoped></style>
