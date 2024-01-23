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
  isRequired: {
    type: Boolean,
    default: false,
  },
  autofocus: {
    type: Boolean,
    default: false,
  },
})
const emit = defineEmits(['input', 'keydown-enter'])

const { value, errorMessage } = useField(() => props.name);
</script>

<template>
  <div class="field text-left">
    <label for="projectIdInput"><span v-if="isRequired">*</span> {{ label }}</label>
    <InputText
      class="w-full"
      type="text"
      v-model="value"
      @input="emit('input', $event.target.value)"
      @keydown.enter="emit('keydown-enter', $event.target.value)"
      :data-cy="name"
      :autofocus="autofocus"
      :id="name"
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