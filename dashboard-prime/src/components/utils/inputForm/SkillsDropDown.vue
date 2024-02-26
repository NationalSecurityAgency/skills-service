<script setup>
import { useField } from 'vee-validate'

const props = defineProps({
  name: {
    type: String,
    required: true,
  },
  label: {
    type: String,
    required: false,
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
defineOptions({
  inheritAttrs: false
})
const { value, errorMessage } = useField(() => props.name);
</script>

<template>
  <div class="m-0 p-0">
    <label v-if="label" :for="name"><span v-if="isRequired">*</span> {{ label }}:</label>
    <Dropdown v-model="value"
              :options="options"
              class="w-full"
              v-bind="$attrs"
              :autofocus="autofocus"
              :id="name"
              :disabled="disabled"
              :class="{ 'p-invalid': errorMessage }"
              :aria-invalid="errorMessage ? null : true"
              :aria-errormessage="`${name}Error`"
              :aria-describedby="`${name}Error`">
      <template #value="slotProps">
        <slot name="value" :value="slotProps.value"></slot>
      </template>
      <template #option="slotProps">
        <slot name="option" :option="slotProps.option"></slot>
      </template>
    </Dropdown>
    <small
        role="alert"
        class="p-error"
        :data-cy="`${name}Error`"
        :id="`${name}Error`">{{ errorMessage || '&nbsp;' }}</small>
  </div>
</template>

<style scoped>

</style>