<script setup>
import { ref, inject } from 'vue'
import { useField } from 'vee-validate'
import InputGroup from 'primevue/inputgroup';
import InputGroupAddon from 'primevue/inputgroupaddon';


const props = defineProps({
  name: {
    type: String,
    required: true
  },
  label: {
    type: String,
    required: true
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['can-edit', 'keydown-enter'])
const canEdit = ref(false)

function notifyAboutEditStateChange(newValue) {
  emit('can-edit', newValue)
  // if (newValue) {
  //   const element = document.getElementById('idInput');
  //   if (element) {
  //     element.focus()
  //   }
  // }
}

const { value, errorMessage } = useField(() => props.name)
const doSubmitForm = inject('doSubmitForm', null)
const onEnter = (event) => {
  if (doSubmitForm) {
    doSubmitForm()
  }
  emit('keydown-enter', event.target.value)
}

const updateIdValue = (newValue) => {
  value.value = newValue
}
defineExpose({
  updateIdValue
})
</script>

<template>
  <div class="field">
    <label for="idInput">* {{ label }}</label>
    <InputGroup>
      <InputGroupAddon>
        <div style="width: 3.3rem !important;">
          <InputSwitch
            v-model="canEdit"
            style="height:1rem !important;"
            size="small"
            name="Enable Id"
            aria-label="Enable ID input to override auto-generated value."
            data-cy="enableIdInput"
            @input="notifyAboutEditStateChange" />
        </div>
      </InputGroupAddon>
    <InputText
      autofocus
      type="text"
      class="w-full"
      :class="{ 'surface-300': !canEdit, 'p-invalid': errorMessage }"
      id="idInput"
      ref="idInput"
      v-model="value"
      @keydown.enter="onEnter"
      :disabled="!canEdit || disabled"
      aria-required="true"
      :aria-invalid="errorMessage ? true : false"
      aria-errormessage="idError"
      aria-describedby="idError"
      data-cy="idInputValue" />

    </InputGroup>
    <!--        @input="dataChanged"-->
    <small role="alert" class="p-error" data-cy="idError" id="idError">{{ errorMessage }}</small>
  </div>
</template>

<style scoped>

</style>