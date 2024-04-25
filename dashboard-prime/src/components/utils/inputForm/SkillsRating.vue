<script setup>
import { useField } from 'vee-validate'
import { useSkillsInputFallthroughAttributes } from "@/components/utils/inputForm/UseSkillsInputFallthroughAttributes.js";
const props = defineProps({
  name: {
    type: String,
    required: true
  },
})
const emit = defineEmits(['update:modelValue'])
const { value, errorMessage } = useField(() => props.name, undefined, {syncVModel: true})
const fallthroughAttributes = useSkillsInputFallthroughAttributes()
</script>

<template>
  <div v-bind="fallthroughAttributes.rootAttrs.value">
    <Rating
        v-bind="fallthroughAttributes.inputAttrs.value"
        v-model="value" />
    <small v-if="errorMessage"
           role="alert"
           class="p-error"
           :data-cy="`${name}Error`"
           :id="`${name}Error`">{{ errorMessage || '&nbsp;' }}</small>
  </div>
</template>

<style scoped>

</style>