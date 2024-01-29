<script setup>
import { ref, watch, computed } from 'vue';
import Dialog from 'primevue/dialog';
import InputText from 'primevue/inputtext';
import {useFocusState} from "@/stores/UseFocusState.js";

const emit = defineEmits(['hidden', 'do-remove']);

const props = defineProps({
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
});

const model = defineModel()

let currentValidationText = ref('');

const removeDisabled = computed(() => {
  return currentValidationText.value !== props.validationText;
});

const focusState = useFocusState()
const publishHidden = (e) => {
  close()
  emit('hidden', { ...e });
};

const removeAction = () => {
  close()
  emit('do-remove');
};

const close =() => {
  model.value = false
  if (props.enableReturnFocus) {
    focusState.focusOnLastElement()
  }
}
</script>

<template>
  <Dialog v-model:visible="model" modal header="Removal Safety Check" @update:visible="publishHidden" :style="{ width: '40rem' }">
    <div class="px-2">
      <div data-cy="removalSafetyCheckMsg">
        <slot />
      </div>

      <div v-if="!removalNotAvailable" >
        <hr />
        <p
            :aria-label="`Please type ${validationText} in the input box to permanently remove the record. To complete deletion press 'Yes, Do Remove' button!`">
          Please type <span class="font-italic font-bold text-primary">{{ validationText }}</span> to permanently
          remove the record.
        </p>
        <InputText v-model="currentValidationText" data-cy="currentValidationText" aria-required="true" style="width: 100%"
                   aria-label="Type 'Delete Me' text here to enable the removal operation. Please make sure that 'D' and 'M' are uppercase." />
      </div>
    </div>

    <template #footer class="w-100">
      <SkillsButton variant="secondary" size="small" class="float-right" @click="publishHidden" data-cy="closeRemovalSafetyCheck" icon="fas fa-times" label="Cancel" />
      <SkillsButton v-if="!removalNotAvailable" severity="danger" size="small" class="float-right ml-2"
                    @click="removeAction" data-cy="removeButton" :disabled="removeDisabled" icon="fas fa-trash" label="Yes, Do Remove!" />
    </template>
  </Dialog>
</template>

<style scoped></style>
