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
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { array, date, number, object, string, tuple } from 'yup'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SkillsNameAndIdInput from '@/components/utils/inputForm/SkillsNameAndIdInput.vue'
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import HelpUrlInput from '@/components/utils/HelpUrlInput.vue'
import SkillsCalendarInput from '@/components/utils/inputForm/SkillsCalendarInput.vue'
import BadgesService from '@/components/badges/BadgesService'
import GlobalBadgeService from '@/components/badges/global/GlobalBadgeService.js'
import InputSanitizer from '@/components/utils/InputSanitizer.js'
import IconPicker from '@/components/utils/iconPicker/IconPicker.vue'
import dayjs from 'dayjs'

const model = defineModel()
const props = defineProps({
  badge: Object,
  isEdit: {
    type: Boolean,
    default: false,
  },
  value: Boolean,
  global: {
    type: Boolean,
    default: false,
  },
});
const appConfig = useAppConfig()
const emit = defineEmits(['hidden', 'badge-updated', 'keydown-enter']);
const route = useRoute()
const announcer = useSkillsAnnouncer()

onMounted(() => {
  document.addEventListener('focusin', trackFocus);
});

let formId = 'newBadgeDialog'
let modalTitle = 'New Badge'
if (props.isEdit) {
  formId = `editBadgeDialog-${route.params.projectId}-${props.badge.badgeId}`
  modalTitle = 'Editing Existing Badge'
}

const maximumDays = computed(() => {
  return appConfig.maxBadgeBonusInMinutes / (60 * 24)
});

const validateTime = (value, testContext) => {
  return true;
  // return ((days * 24 * 60) + (hours * 60) + minutes) <= this.$store.getters.config.maxBadgeBonusInMinutes;
}

const maxTimeLimitMessage = computed(() => {
  return `Time Window must be less then ${appConfig.maxBadgeBonusInMinutes / (60 * 24)} days`;
});

const schema = object({
  'name': string()
      .trim()
      .required()
      .min(appConfig.minNameLength)
      .max(appConfig.maxBadgeNameLength)
      .nullValueNotAllowed()
      .test('uniqueName', 'Badge Name is already taken', (value) => checkBadgeNameUnique(value))
      .customNameValidator('Badge Name')
      .label('Badge Name'),
  'badgeId': string()
      // .trim()
      .required()
      .min(appConfig.minIdLength)
      .max(appConfig.maxIdLength)
      .nullValueNotAllowed()
      .idValidator()
      .test('uniqueId', 'Badge ID is already taken', (value) => checkBadgeIdUnique(value))
      .label('Badge ID'),
  'description': string()
      .max(appConfig.descriptionMaxLength)
      .customDescriptionValidator('Badge Description')
      .label('Badge Description'),
  'helpUrl': string()
      .urlValidator()
      .label('Help URL'),
  'awardAttrs.name': string()
      .label('Award Name')
      .min(appConfig.minNameLength)
      .max(appConfig.maxBadgeNameLength),
  'expirationHrs': number()
      .label('Expiration Hours')
      .min(0)
      .max(23),
  'expirationMins': number()
      .label('Expiration Minutes')
      .min(0)
      .max(59),
  'expirationDays': number()
      .label('Expiration Days')
      .min(0)
      .when(['expirationMins', 'expirationHrs'],  {
        is: (expirationMins, expirationHrs) => expirationHrs > 0 || expirationMins > 0,
        then: (sch) => sch.max(maximumDays.value - 1),
        otherwise: (sch) => sch.max(maximumDays.value)
      }),
  'gemDates': tuple([
      date().label('Start date').required('You must select a start date'),
      date().label('End date').required('You must select an end date')
  ]).label('Gem Date').test('notInPast', 'End date can not be in the past', (value) => {
    let valid = true;
    // only trigger this validation on new badge entry, not edits
    if(value && value.length === 2) {
      const [startDate, endDate] = value;
      if (limitTimeframe.value && endDate && !badgeInternal.value.badgeId) {
        valid = dayjs(endDate).isAfter(dayjs());
      }
    }
    return valid;
  })

});

let awardAttrs = {
  name: 'Speedy Finish',
  iconClass: 'fas fa-car-side',
  numMinutes: 0,
};

if (props.badge.awardAttrs && props.badge.awardAttrs.name !== null) {
  awardAttrs = props.badge.awardAttrs;
}

const minutesPerDay = 24 * 60;
const expirationDays = awardAttrs.numMinutes ? Math.floor((awardAttrs.numMinutes / minutesPerDay)) : 0;
const remainingHrs = awardAttrs.numMinutes - (expirationDays * minutesPerDay);
const expirationHrs = remainingHrs ? Math.floor((remainingHrs / 60)) : 8;
const expirationMins = remainingHrs ? remainingHrs % 60 : 0;
const limitedTimeframe = !!(props.badge.startDate && props.badge.endDate);
const timeLimitEnabled = awardAttrs.numMinutes > 0;
const initialBadgeData = {
  badgeId: props.badge.badgeId,
  name: props.badge.name || '',
  description: props.badge.description || '',
  helpUrl: props.badge.helpUrl || '',
  startDate: toDate(props.badge.startDate),
  endDate: toDate(props.badge.endDate),
  expirationDays: expirationDays,
  expirationHrs: expirationHrs,
  expirationMins: expirationMins,
  timeLimitEnabled: timeLimitEnabled,
  awardAttrs: awardAttrs,
  iconClass: props.badge.iconClass || 'fas fa-book',
  projectId: route.params.projectId
};

let badgeInternal = ref({
  originalBadgeId: props.badge.badgeId,
  isEdit: props.isEdit,
  description: '',
  startDate: props.badge.startDate,
  endDate: props.badge.endDate,
  badgeId: props.badge.badgeId,
  awardAttrs: awardAttrs,
  expirationDays,
  expirationHrs,
  expirationMins,
  timeLimitEnabled,
  ...props.badge,
});

const limitTimeframe = ref(limitedTimeframe);
const currentFocus = ref( null);
const previousFocus = ref( null);
const gemDates = ref([toDate(props.badge.startDate), toDate(props.badge.endDate)]);
const currentIcon = ref((props.badge.iconClass || 'fas fa-book'));
const awardIcon = ref(props.badge.awardAttrs?.iconClass || 'fas fa-car-side');

// computed
const checkBadgeNameUnique = (value) => {
  if(!value) {
    return true;
  }

  if (props.isEdit && (value === props.badge.name || props.badge.name.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
    return true;
  }
  if (props.global) {
    return GlobalBadgeService.badgeWithNameExists(value);
  }
  return BadgesService.badgeWithNameExists(route.params.projectId, value);
}

const checkBadgeIdUnique = (value) => {
  if(!value) {
    return true;
  }

  if (props.isEdit && props.badge.badgeId === value) {
    return true;
  }
  if (props.global) {
    return GlobalBadgeService.badgeWithIdExists(value);
  }
  return BadgesService.badgeWithIdExists(route.params.projectId, value);
};

// methods
const trackFocus = () => {
  previousFocus.value = currentFocus.value;
  currentFocus.value = document.activeElement;
};

const updateBadge = (values) => {
  let badgeToSave = {
    ...values,
    iconClass: currentIcon.value,
    originalBadgeId: props.badge.badgeId,
    projectId: route.params.projectId,
    isEdit: props.isEdit,
    name: InputSanitizer.sanitize(values.name),
    subjectId: InputSanitizer.sanitize(values.badgeId)
  };

  if(badgeInternal.value.timeLimitEnabled) {
    badgeToSave.timeLimitEnabled = true;
    badgeToSave.awardAttrs = values.awardAttrs;
    badgeToSave.awardAttrs.iconClass = awardIcon.value;
  }

  if(limitTimeframe.value) {
    badgeToSave.startDate = gemDates.value[0];
    badgeToSave.endDate = gemDates.value[1];
  } else {
    badgeToSave.startDate = null;
    badgeToSave.endDate = null;
  }

  if(props.global) {
    return GlobalBadgeService.saveBadge(badgeToSave).then((resp) => {
      badgeInternal.value = resp;
    })
  }
  return BadgesService.saveBadge(badgeToSave).then((resp) => {
    badgeInternal.value = resp;
  });
};

const onSelectedIcon = (selectedIcon, isAward = false) => {
  if (isAward) {
    awardIcon.value = selectedIcon.css;
  } else {
    currentIcon.value = selectedIcon.css;
  }
};

const onEnableGemFeature = () => {
  nextTick(() => {
    gemDates.value = [];
  })
};

function toDate(value) {
  let dateVal = value;
  if (value && !(value instanceof Date)) {
    dateVal = new Date(Date.parse(value.replace(/-/g, '/')));
  }
  return dateVal;
}


const resetTimeLimit = (checked) => {
  if (!checked) {
    badgeInternal.value.expirationDays = 0;
    badgeInternal.value.expirationHrs = 8;
    badgeInternal.value.expirationMins = 0;
  }
};

const close = (e) => {
  emit('hidden', e);
  model.value = false
}

const onBadgeSaved = () => {
  emit('badge-updated', badgeInternal.value );
  close();
};

</script>

<template>
  <SkillsInputFormDialog
      :id="formId"
      v-model="model"
      :header="modalTitle"
      saveButtonLabel="Save"
      :validation-schema="schema"
      :initial-values="initialBadgeData"
      :save-data-function="updateBadge"
      :enable-return-focus="true"
      @saved="onBadgeSaved"
      @close="close">
    <template #default>
      <SkillsNameAndIdInput
          name-label="Badge Name"
          name-field-name="name"
          id-label="Badge ID"
          id-field-name="badgeId"
          id-suffix="Badge"
          :name-to-id-sync-enabled="!props.isEdit">
        <template #beforeName>
          <icon-picker
            class="mb-3"
            :startIcon="currentIcon"
            @selected-icon="onSelectedIcon"
          />
        </template>
      </SkillsNameAndIdInput>

      <markdown-editor class="mt-5" name="description" />

      <Card v-if="!global" data-cy="bonusAwardCard">
        <template #content>
          <div>
            <div class="pb-3">
              <Checkbox data-cy="timeLimitCheckbox" id="checkbox-1" inputId="enableAward" class="d-inline" :binary="true" name="timeLimitEnabled" v-model="badgeInternal.timeLimitEnabled" v-on:input="resetTimeLimit"/>
              <label for="enableAward">
                Enable Bonus Award
              </label>
            </div>
            <div class="flex gap-2" style="padding-bottom: 10px;" v-if="badgeInternal.timeLimitEnabled">
              <div>
                <icon-picker :startIcon="awardIcon" class="mr-3" @selected-icon="(e) => onSelectedIcon(e, true)" :disabled="false"></icon-picker>
              </div>
              <div class="w-full">
                <label for="awardName">Award Name</label>
                <SkillsTextInput
                    placeholder="Award Name"
                    id="awardName"
                    name="awardAttrs.name"/>
              </div>
            </div>
            <div class="flex gap-4" v-if="badgeInternal.timeLimitEnabled">
              <div class="flex flex-1">
                <SkillsNumberInput v-model="badgeInternal.expirationDays"
                                   class="w-full"
                                   label="Days"
                                   id="timeLimitDays"
                                   :min="0"
                                   data-cy="timeLimitDays"
                                   name="expirationDays" />
              </div>
              <div class="flex flex-1">
                <SkillsNumberInput v-model="badgeInternal.expirationHrs"
                                   class="w-full"
                                   label="Hours"
                                   id="timeLimitHours"
                                   :min="0"
                                   :max="23"
                                   data-cy="timeLimitHours"
                                   name="expirationHrs" />
              </div>
              <div class="flex flex-1">
                <SkillsNumberInput v-model="badgeInternal.expirationMins"
                                   class="w-full"
                                   label="Minutes"
                                   id="timeLimitMinutes"
                                   :min="0"
                                   :max="59"
                                   data-cy="timeLimitMinutes"
                                   name="expirationMins" />
              </div>
            </div>
          </div>
        </template>
      </Card>

      <help-url-input class="mt-3"
                      :next-focus-el="previousFocus"
                      name="helpUrl"
                      @keydown-enter="emit('keydown-enter')" />

      <div v-if="!global" data-cy="gemEditContainer">
        <Checkbox data-cy="gemCheckbox" inputId="enableGem" class="d-inline" :binary="true" name="gemCheckbox" v-model="limitTimeframe" v-on:change="onEnableGemFeature"/>
        <label for="enableGem">
          Enable Gem Feature
        </label>

        <div v-if="limitTimeframe" class="flex justify-content-center gap-4">
          <div>
            <SkillsCalendarInput selectionMode="range" inline name="gemDates" v-model="gemDates" label="Date Range" />
          </div>
        </div>
      </div>


    </template>
  </SkillsInputFormDialog>
</template>

<style scoped>
i {
  font-size: 3rem;
}

.icon-button {
  cursor: pointer;
  height: 100px;
  width: 100px;
  background-color: transparent;
}

.icon-button:disabled {
  background-color: lightgrey;
  cursor: none;
}
</style>