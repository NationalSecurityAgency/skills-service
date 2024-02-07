<script setup>
import { useField } from 'vee-validate'
import InlineHelp from "@/components/utils/InlineHelp.vue";

const props = defineProps({
  name: {
    type: String,
    required: true,
  },
  label: {
    type: String,
    required: true,
  },
  autofocus: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false
  },
  helpMessage: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: ''
  },
})
const emit = defineEmits(['input'])

const { value, errorMessage } = useField(() => props.name);

</script>
<!--v-bind="projectDisplayNameAttrs"-->
<template>
  <div class="flex flex-row">
    <div class="md:col-5 xl:col-3 text-secondary" :id="`${name}Label`">
      <label :for="name">
        {{ label }}
      </label>
      <inline-help :target-id="`${name}Help`" :msg='helpMessage'/>
    </div>
    <div class="md:col-7 xl:col-9">
      <InputText v-model="value"
                 :data-cy="`${name}TextInput`"
                 :id="name"
                 type="text"
                 @input="emit('input', [name, $event.target.value])"
                 class="w-full"
                 :placeholder="placeholder"
                 :class="{ 'p-invalid': errorMessage }"
                 :aria-invalid="errorMessage ? null : true"
                 :aria-errormessage="`${name}Error`"
                 :aria-describedby="`${name}Error`"
                 :aria-labelledby="`${name}Label`" />
      <small class="p-error" :id="`${name}Error`" :data-cy="`${name}Error`" v-if="errorMessage">{{ errorMessage || '&nbsp;' }}</small>
    </div>
  </div>
</template>

<style scoped>

</style>