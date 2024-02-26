<script setup>
import { ref, computed } from 'vue';
import SkillsDialog from '@/components/utils/inputForm/SkillsDialog.vue';
import InputText from 'primevue/inputtext';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import { useFocusState } from '@/stores/UseFocusState.js'

const focusState = useFocusState()

const emit = defineEmits(['hidden', 'do-remove']);

const props = defineProps({
  itemName: {
    type: String,
    required: true,
  },
  itemType: {
    type: String,
    required: true,
  },
  validationText: {
    type: String,
    required: false,
    default: 'Delete Me',
  },
  removalNotAvailable: {
    type: Boolean,
    required: false,
    default: false,
  },
  enableReturnFocus: {
    type: Boolean,
    default: false
  },
  focusOnCloseId: {
    type: String,
    required: true,
  },
  loading: {
    type: Boolean,
    default: false
  },
});

const model = defineModel()

let currentValidationText = ref('');

const removeDisabled = computed(() => {
  return currentValidationText.value !== props.validationText;
});

const publishHidden = (e) => {
  close()
  emit('hidden', { ...e });
};

const removeAction = () => {
  if (Boolean(props.focusOnCloseId)) {
    focusState.setElementId(props.focusOnCloseId);
  }
  close()
  emit('do-remove');
};

const close =() => {
  model.value = false
}
</script>

<template>
  <SkillsDialog
      :maximizable="false"
      v-model="model"
      header="Removal Safety Check"
      cancel-button-severity="secondary"
      ok-button-severity="danger"
      :ok-button-icon="'fas fa-trash'"
      ok-button-label="Yes, Do Remove!"
      :ok-button-disabled="removeDisabled"
      :show-ok-button="!removalNotAvailable"
      @on-ok="removeAction"
      @on-cancel="publishHidden"
      :enable-return-focus="enableReturnFocus || Boolean(focusOnCloseId)"
      :style="{ width: '40rem !important' }">
    <skills-spinner v-if="loading" :is-loading="loading" class="my-4"/>
    <div v-if="!loading" class="px-2">
      <div data-cy="removalSafetyCheckMsg">
        <div v-if="!removalNotAvailable">
          This will remove <span
          class="font-bold text-primary">{{ itemName }}</span> {{ itemType}}.
        </div>
        <Message severity="warn" :closable="false">
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
