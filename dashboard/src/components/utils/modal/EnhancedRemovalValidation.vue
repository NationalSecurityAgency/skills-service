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
import { ref, computed, watch } from 'vue';
import { useFocusState } from '@/stores/UseFocusState.js'
import { useSlotsUtil } from '@/components/utils/UseSlotsUtil.js';
import SkillsDialog from '@/components/utils/inputForm/SkillsDialog.vue';
import InputText from 'primevue/inputtext';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import Stepper from 'primevue/stepper'
import StepperPanel from 'primevue/stepperpanel'

const focusState = useFocusState()
const slotsUtil = useSlotsUtil();

const emit = defineEmits(['hidden', 'do-remove']);

const props = defineProps({
  itemName: {
    type: String,
    required: true,
  },
  itemType: {
    type: String,
    required: false,
  },
  validationText: {
    type: String,
    required: false,
    default: 'Delete This Project',
  },
  removeButtonLabel: {
    type: String,
    required: false,
    default: 'Yes, Do Remove!',
  },
  removalNotAvailable: {
    type: Boolean,
    required: false,
    default: false,
  },
  focusOnCloseId: {
    type: String,
    required: false,
  },
  loading: {
    type: Boolean,
    default: false
  },
  removalTextPrefix: {
    type: String,
    required: false,
    default: 'This will delete the',
  },
});

const model = defineModel()

let currentValidationText = ref('');

const removeDisabled = computed(() => {
  return currentValidationText.value !== props.validationText;
});
const announcer = useSkillsAnnouncer()
watch(removeDisabled, (newValue) => {
  if(!newValue) {
    announcer.polite(`Removal operation successfully enabled. Please click on ${props.removeButtonLabel} button`)
  }
})

const publishHidden = (e) => {
  close()
  emit('hidden', { ...e });
};

const removeAction = () => {
  if (props.focusOnCloseId) {
    focusState.setElementId(props.focusOnCloseId);
  }
  close()
  emit('do-remove');
};

const close = () => {
  model.value = false
}

const confirmStepOne = ref(false);
const confirmStepTwo = ref(false);

const clearSettings = () => {
  confirmStepOne.value = false;
  confirmStepTwo.value = false;
  currentValidationText.value = '';
}
</script>

<template>
  <SkillsDialog
      :maximizable="false"
      v-model="model"
      header="Removal Safety Check"
      cancel-button-severity="secondary"
      :show-ok-button="false"
      :show-cancel-button="false"
      @on-ok="removeAction"
      @on-cancel="publishHidden"
      :enable-return-focus="true"
      :style="{ width: '40rem !important' }">
    <skills-spinner v-if="loading" :is-loading="loading" class="my-4"/>
    <div v-if="!loading" class="px-2">
      <Stepper linear @step-change="clearSettings">
        <StepperPanel>
          <template #content="{ nextCallback }">
            <div data-cy="removalSafetyCheckMsg">
              <div v-if="!removalNotAvailable">
                {{ removalTextPrefix }} <span class="font-bold text-primary">{{ itemName }}</span><span v-if="itemType">&nbsp;{{ itemType }}</span>.
              </div>
              <slot name="initialMessage"></slot>
              <div class="flex flex-1">
                <Checkbox inputId="stepOneCheck" :binary="true" name="Confirm" v-model="confirmStepOne" />
                <label for="stepOneCheck" class="ml-2">I understand that this is permanent and cannot be undone</label>
              </div>
              <div class="flex flex-1 relative mt-2">
                <SkillsButton label="Cancel" />
                <SkillsButton label="Next" class="right-0 absolute" icon="fas fa-arrow-circle-right" @click="nextCallback" :disabled="!confirmStepOne"/>
              </div>
            </div>
          </template>
        </StepperPanel>
        <StepperPanel>
          <template #content="{ prevCallback, nextCallback }">
            <div>
              <slot name="userMessage"></slot>
              <div class="flex flex-1 mt-2">
                <Checkbox
                    inputId="stepTwoCheck"
                    :binary="true"
                    name="Confirm"
                    v-model="confirmStepTwo"
                />
                <label for="stepTwoCheck" class="ml-2">I understand that this project will be deleted for ALL users.</label>
              </div>
              <div class="flex mt-2 relative">
                <SkillsButton label="Back" icon="fas fa-arrow-circle-left" @click="prevCallback" />
                <SkillsButton label="Next" icon="fas fa-arrow-circle-right" class="absolute right-0" @click="nextCallback" :disabled="!confirmStepTwo" />
              </div>
            </div>
          </template>
        </StepperPanel>
        <StepperPanel>
          <template #content="{ prevCallback }">
            <Message severity="warn" :closable="false">
              Are you SURE you want to delete this project? Remember: This action is permanent and can not be recovered!
            </Message>
            <div v-if="!removalNotAvailable" class="mb-4">
              <p
                  :aria-label="`Please type ${validationText} in the input box to permanently remove the record. To complete deletion press 'Yes, Do Remove' button!`">
                Please type <span class="font-italic font-bold text-primary">{{ validationText }}</span> to permanently
                remove the record.
              </p>
              <InputText v-model="currentValidationText" data-cy="currentValidationText" aria-required="true" style="width: 100%"
                         aria-label="Type 'Delete This Project' text here to enable the removal operation. Please make sure that 'D' and 'M' are uppercase." />
            </div>
            <div class="flex flex-1 mt-2 relative">
              <SkillsButton label="Back" icon="fas fa-arrow-circle-left" @click="prevCallback" />
              <SkillsButton label="Delete" class="absolute right-0" @click="removeAction" :disabled="removeDisabled" />
            </div>
          </template>
        </StepperPanel>
      </Stepper>

    </div>
  </SkillsDialog>
</template>

<style scoped></style>
