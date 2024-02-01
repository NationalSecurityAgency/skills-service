<script setup>
import { useField } from 'vee-validate'

const props = defineProps({
  name: {
    type: String,
    required: true,
  },
  label: {
    type: String,
    required: true,
  },
  options: {
    type: Object,
    required: true,
  },
  isRequired: {
    type: Boolean,
    default: false,
  },
  autofocus: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false
  },
})
const { value, errorMessage } = useField(() => props.name);
</script>

<template>
  <div class="field text-left">
    <label :for="name"><span v-if="isRequired">*</span> {{ label }}:</label>
    <Dropdown v-model="value"
              :options="options"
              class="w-full"
              :data-cy="name"
              :autofocus="autofocus"
              :id="name"
              :disabled="disabled"
              :class="{ 'p-invalid': errorMessage }"
              :aria-invalid="errorMessage ? null : true"
              :aria-errormessage="`${name}Error`"
              :aria-describedby="`${name}Error`" />
    <small
        role="alert"
        class="p-error"
        :data-cy="`${name}Error`"
        :id="`${name}Error`">{{ errorMessage || '&nbsp;' }}</small>
  </div>
</template>

<style scoped>

</style>