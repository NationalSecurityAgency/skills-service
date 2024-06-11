<script setup>
import { ref, computed, watch } from 'vue';
import { useFocusState } from '@/stores/UseFocusState.js'
import { useSlotsUtil } from '@/components/utils/UseSlotsUtil.js';
import SkillsDialog from '@/components/utils/inputForm/SkillsDialog.vue';
import InputText from 'primevue/inputtext';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'

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
    default: 'Delete Me',
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
    default: 'This will remove',
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
const hasSlot = computed(() => {
  return slotsUtil.hasSlot()
})
</script>

<template>
  <SkillsDialog
      :maximizable="false"
      v-model="model"
      header="Removal Safety Check"
      cancel-button-severity="secondary"
      ok-button-severity="danger"
      :ok-button-icon="'fas fa-trash'"
      :ok-button-label="removeButtonLabel"
      :ok-button-disabled="removeDisabled"
      :show-ok-button="!removalNotAvailable"
      @on-ok="removeAction"
      @on-cancel="publishHidden"
      :enable-return-focus="true"
      :style="{ width: '40rem !important' }">
    <skills-spinner v-if="loading" :is-loading="loading" class="my-4"/>
    <div v-if="!loading" class="px-2">
      <div data-cy="removalSafetyCheckMsg">
        <div v-if="!removalNotAvailable">
          {{ removalTextPrefix }} <span
          class="font-bold text-primary">{{ itemName }}</span><span v-if="itemType">&nbsp;{{ itemType }}</span>.
        </div>
        <Message v-if="hasSlot" severity="warn" :closable="false">
          <div class="pl-2"><slot /></div>
        </Message>
      </div>

      <div v-if="!removalNotAvailable" class="mb-4">
        <p
            :aria-label="`Please type ${validationText} in the input box to permanently remove the record. To complete deletion press 'Yes, Do Remove' button!`">
          Please type <span class="font-italic font-bold text-primary">{{ validationText }}</span> to permanently
          remove the record.
        </p>
        <InputText v-model="currentValidationText" data-cy="currentValidationText" aria-required="true" style="width: 100%"
                   aria-label="Type 'Delete Me' text here to enable the removal operation. Please make sure that 'D' and 'M' are uppercase." />
      </div>
    </div>
  </SkillsDialog>
</template>

<style scoped></style>
