<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRoute } from "vue-router";
import SkillsInputFormDialog from "@/components/utils/inputForm/SkillsInputFormDialog.vue";
import { useAppConfig } from "@/common-components/stores/UseAppConfig.js";
import {number, string} from "yup";
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SkillsNameAndIdInput from "@/components/utils/inputForm/SkillsNameAndIdInput.vue";
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import HelpUrlInput from "@/components/utils/HelpUrlInput.vue";
import OverlayPanel from "primevue/overlaypanel";
import IconManager from "@/components/utils/iconPicker/IconManager.vue";
import BadgesService from '@/components/badges/BadgesService';
import InputSanitizer from "@/components/utils/InputSanitizer.js";

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
const emit = defineEmits(['hidden', 'badge-saved']);
const route = useRoute()
const announcer = useSkillsAnnouncer();

let formId = 'newBadgeDialog'
let modalTitle = 'New Badge'
if (props.isEdit) {
  formId = `editBadgeDialog-${route.params.projectId}-${props.badge.badgeId}`
  modalTitle = 'Editing Existing Badge'
}

const schema = {
  'name': string()
      .trim()
      .required()
      .min(appConfig.minNameLength)
      // .max(appConfig.maxSubjectNameLength)
      .nullValueNotAllowed()
      .test('uniqueName', 'Badge Name is already taken', (value) => checkBadgeNameUnique(value))
      .customNameValidator()
      .label('Badge Name'),
  'badgeId': string()
      .trim()
      .required()
      .min(appConfig.minIdLength)
      .max(appConfig.maxIdLength)
      .nullValueNotAllowed()
      .idValidator()
      // .test('uniqueName', 'Subject ID is already taken', (value) => checkBadgeIdUnique(value))
      .customNameValidator()
      .label('Subject ID'),
  'description': string()
      .max(appConfig.descriptionMaxLength)
      .customDescriptionValidator('Badge Description')
      .label('Badge Description'),
  'helpUrl': string()
      .urlValidator()
      .label('Help URL'),
  'expirationDays': number().label('Expiration Days'),
  'expirationHrs': number().label('Expiration Hours'),
  'expirationMins': number().label('Expiration Minutes')

};

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
  startDate: null,
  endDate: null,
  badgeId: props.badge.badgeId,
  awardAttrs: awardAttrs,
  expirationDays,
  expirationHrs,
  expirationMins,
  timeLimitEnabled,
  ...props.badge,
});

let limitTimeframe = ref(limitedTimeframe);
let currentFocus = ref( null);
let previousFocus = ref( null);
let loadingComponent = ref( true);
let isAwardIcon = ref(false);

let currentIcon = ref((props.badge.iconClass || 'fas fa-book'));
let awardIcon = ref(props.badge.awardAttrs?.iconClass);
let op = ref();

// computed
const checkBadgeNameUnique = (value) => {
  if(!value) {
    return true;
  }

  if (props.isEdit && (value === props.badge.name || props.badge.name.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
    return true;
  }
  // if (props.global) {
  //   return GlobalBadgeService.badgeWithNameExists(value);
  // }
  return BadgesService.badgeWithNameExists(route.params.projectId, value);
}

// const componentName = computed(() => {
//   const badgeScope = badgeInternal.value.projectId ? badgeInternal.value.projectId : 'Global';
//   return `${badgeScope}-${$options.name}${isEdit ? 'Edit' : ''}`;
// });

const maxTimeLimitMessage = computed(() => {
  return `Time Window must be less then ` //${$appConfig.maxBadgeBonusInMinutes / (60 * 24)} days`;
});
//
// // methods
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
    badgeToSave.expirationDays = badgeInternal.value.expirationDays;
    badgeToSave.expirationHrs = badgeInternal.value.expirationHrs;
    badgeToSave.expirationMins = badgeInternal.value.expirationMins;
  }

  return BadgesService.saveBadge(badgeToSave).then((resp) => {
    emit('badge-updated', { isEdit: props.isEdit, ...resp });
  });

  // $refs.observer.validate()
  //     .then((res) => {
  //       if (res) {
  //   publishHidden({ updated: true });
  //   badgeInternal.value.badgeId = InputSanitizer.sanitize(badgeInternal.value.badgeId);
  //   badgeInternal.value.name = InputSanitizer.sanitize(badgeInternal.value.name);
  // emit('badge-updated', { isEdit: isEdit, ...badgeInternal.value });
      //   }
      // });
};

const onSelectedIcon = (selectedIcon) => {

  if (isAwardIcon.value) {
    awardIcon.value = selectedIcon.css;
    // badgeInternal.value.awardAttrs.iconClass = `${selectedIcon.css}`;
  } else {
    currentIcon.value = selectedIcon.css;
  }
  op.value.hide();
};

const onEnableGemFeature = (value) => {
  if (!value) {
    nextTick(() => {
      badgeInternal.value.startDate = null;
      badgeInternal.value.endDate = null;
    });
  }
};

function toDate(value) {
  let dateVal = value;
  if (value && !(value instanceof Date)) {
    dateVal = new Date(Date.parse(value.replace(/-/g, '/')));
  }
  return dateVal;
}

const toggleIconDisplay = (event, isAward) => {
  // displayIconManager.value = shouldDisplay;
  op.value.toggle(event);
  isAwardIcon.value = isAward;
};

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
</script>

<!--      @saved="onBadgeSaved"-->
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
      @close="close">
    <template #default>
      <button class="icon-button surface-border border-round-sm"
              @click="toggleIconDisplay"
              id="iconPicker"
              @keypress.enter="toggleIconDisplay"
              role="button"
              aria-roledescription="icon selector button"
              aria-label="icon selector"
              tabindex="0"
              data-cy="iconPicker">
        <div class="text-primary" style="min-height: 4rem;">
          <i :class="[currentIcon]" />
        </div>
      </button>

      <SkillsNameAndIdInput
          name-label="Badge Name"
          name-field-name="name"
          id-label="Badge ID"
          id-field-name="badgeId"
          id-suffix="Badge"
          :name-to-id-sync-enabled="!props.isEdit" />

      <markdown-editor class="mt-5" name="description" />

      <Card v-if="!global" data-cy="bonusAwardCard">
        <template #content>
          <div>
            <div>
              <Checkbox data-cy="timeLimitCheckbox" id="checkbox-1" inputId="enableAward" class="d-inline" :binary="true" name="timeLimitEnabled" v-model="badgeInternal.timeLimitEnabled" v-on:input="resetTimeLimit"/>
              <label for="enableAward">
                Enable Bonus Award
              </label>
            </div>
            <div class="grid" style="padding-bottom: 10px;" v-if="badgeInternal.timeLimitEnabled">
              <div class="text-left col">
<!--                  <icon-picker :startIcon="badgeInternal.awardAttrs.iconClass" class="mr-3" @select-icon="toggleIconDisplay(true, true)" :disabled="!badgeInternal.timeLimitEnabled"></icon-picker>-->
                <div>
                  <label for="awardName">Award Name</label>
                  <SkillsTextInput
                      placeholder="Award Name"
                      v-model="badgeInternal.awardAttrs.name"
                      data-cy="awardName"
                      id="awardName"
                      name="awardAttrs.name"/>
  <!--                    <ValidationProvider rules="required|minNameLength|maxBadgeNameLength|customNameValidator"-->
                </div>
              </div>
            </div>
            <div class="flex gap-4" v-if="badgeInternal.timeLimitEnabled">
              <div class="flex flex-1">
  <!--              <ValidationProvider rules="optionalNumeric|required|min_value:0|daysMaxTimeLimit:@timeLimitHours,@timeLimitMinutes|cantBe0IfHours0Minutes0" vid="timeLimitDays" v-slot="{errors}" name="Days">-->
                  <InputGroup>
                    <InputNumber
                        v-model="badgeInternal.expirationDays"
                        :initial-value="initialBadgeData.expirationDays"
                        data-cy="timeLimitDays"
                        id="timeLimitDays"
                        name="expirationDays"/>
                    <InputGroupAddon>
                      Days
                    </InputGroupAddon>
                  </InputGroup>
              </div>
              <div class="flex flex-1">
  <!--              <ValidationProvider rules="optionalNumeric|required|min_value:0|max_value:23|hoursMaxTimeLimit:@timeLimitDays,@timeLimitMinutes|cantBe0IfMins0Days0" vid="timeLimitHours" v-slot="{errors}" name="Hours">-->
                  <InputGroup>
                    <InputNumber
                        v-model="badgeInternal.expirationHrs"
                        :initial-value="initialBadgeData.expirationHrs"
                        data-cy="timeLimitHours"
                        id="timeLimitHours"
                        name="expirationHrs"/>
                    <InputGroupAddon>
                      Hours
                    </InputGroupAddon>
                  </InputGroup>
              </div>
              <div class="flex flex-1">
  <!--              <ValidationProvider rules="optionalNumeric|required|min_value:0|max_value:59|minutesMaxTimeLimit:@timeLimitDays,@timeLimitHours|cantBe0IfHours0Days0" vid="timeLimitMinutes" v-slot="{errors}" name="Minutes">-->
                <InputGroup>
                  <InputNumber
                      v-model="badgeInternal.expirationMins"
                      :initial-value="initialBadgeData.expirationMins"
                      data-cy="timeLimitMinutes"
                      id="timeLimitMinutes"
                      name="expirationMins"/>
                  <InputGroupAddon>
                    Minutes
                  </InputGroupAddon>
                </InputGroup>
              </div>
            </div>
          </div>
        </template>
      </Card>

      <help-url-input class="mt-3"
                      :next-focus-el="previousFocus"
                      name="helpUrl"
                      @keydown-enter="emit('keydown-enter')" />

      <OverlayPanel ref="op" :show-close-icon="true">
        <icon-manager @selected-icon="onSelectedIcon" name="iconClass"></icon-manager>
      </OverlayPanel>
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