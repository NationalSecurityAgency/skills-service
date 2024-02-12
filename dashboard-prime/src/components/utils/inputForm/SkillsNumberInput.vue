<script setup>
import { computed, inject, useAttrs } from 'vue'
import { useField } from 'vee-validate'

const props = defineProps({
  name: {
    type: String,
    required: true
  },
  label: {
    type: String,
    required: true
  },
  isRequired: {
    type: Boolean,
    default: false
  },
  autofocus: {
    type: Boolean,
    default: false
  },
})
const emit = defineEmits(['input', 'keydown-enter'])

const { value, errorMessage } = useField(() => props.name)
const doSubmitForm = inject('doSubmitForm', null)
const onEnter = (event) => {
  if (doSubmitForm) {
    doSubmitForm()
  }
  emit('keydown-enter', event.target.value)
}

const filterAttrs = ['class'];
const attrs = useAttrs()
const inputNumFallthroughAttrs = computed(() =>{
  return Object.fromEntries(
    Object.entries(attrs).filter(
      ([key])=> !filterAttrs.includes(key)
    )
  );
})
</script>

<template>
  <div class="field">
    <label :for="`input${name}`" class="block"><span v-if="isRequired">*</span> {{ label }} </label>
    <InputNumber
      inputClass="w-3rem"
      class="w-full"
      type="number"
      v-bind="inputNumFallthroughAttrs"
      v-model="value"
      @keydown.enter="onEnter"
      :data-cy="name"
      :autofocus="autofocus"
      :id="name"
      :inputId="`input${name}`"
      :class="{ 'p-invalid': errorMessage }"
      :aria-invalid="errorMessage ? null : true"
      :aria-errormessage="`${name}Error`"
      :aria-describedby="`${name}Error`" />
    <small
      role="alert"
      class="p-error block"
      :data-cy="`${name}Error`"
      :id="`${name}Error`">{{ errorMessage || '' }}</small>
  </div>
</template>

<style scoped>

</style>