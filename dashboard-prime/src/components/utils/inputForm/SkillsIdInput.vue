<script setup>
import {ref} from 'vue'
import { useField } from 'vee-validate';

const props = defineProps({
  name: String,
  label: String,
  isSkillId: {
    type: Boolean,
    default: false,
  },
  additionalValidationRules: [String],
  nextFocusEl: HTMLElement })

const emit = defineEmits(['can-edit', 'keydown-enter'])
const canEdit = ref(false)
function notifyAboutEditStateChange(newValue) {
  emit('can-edit', newValue);
}

const { value, errorMessage } = useField(() => props.name);

const updateIdValue = (newValue) => {
  value.value = newValue
}
defineExpose({
  updateIdValue
})
</script>

<template>
  <div class="field">
    <div class="flex mb-2">
    <label for="idInput">* {{ label }}</label>
    <div class="flex-1 text-right justify-content-center align-items-center" data-cy="idInputEnableControl">
      <div class="flex justify-content-end align-items-center">
      <InputSwitch
        v-model="canEdit"
        :disabled="canEdit"
        class="mr-1"
        size="small"
        name="Enable Id"
        aria-label="Enable ID input to override auto-generated value."
        data-cy="enableIdInput"
        @input="notifyAboutEditStateChange" />
<!--        <i class="fas fa-question-circle mr-1 text-xl"-->
<!--           id="idInputHelp"-->
<!--           aria-label="Enable ID input to override auto-generated value."-->
<!--           role="tooltip"-->
<!--           tabindex="0"-->
<!--           v-tooltip="'Enable to override auto-generated value.'" />-->
      </div>
      </div>
    </div>
      <InputText
        type="text"
        class="w-full"
        :class="{ 'surface-300': !canEdit, 'p-invalid': errorMessage }"
        id="idInput"
        v-model="value"
        @keydown.enter="emit('keydown-enter', $event.target.value)"
        :disabled="!canEdit"
        aria-required="true"
        :aria-invalid="errorMessage ? true : false"
        aria-errormessage="idError"
        aria-describedby="idError"
        data-cy="idInputValue" />
<!--        @input="dataChanged"-->
      <small role="alert" class="p-error" data-cy="idError" id="idError">{{ errorMessage }}</small>
<!--    </ValidationProvider>-->
  </div>
</template>

<style scoped>

</style>