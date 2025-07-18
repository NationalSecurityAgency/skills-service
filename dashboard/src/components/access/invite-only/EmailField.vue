/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script setup>
import { ref } from 'vue'
import {useSkillsInputFallthroughAttributes} from "@/components/utils/inputForm/UseSkillsInputFallthroughAttributes.js";
import Textarea from "primevue/textarea";

const props = defineProps({
  field: {
    type: String,
    required: true,
  },
  label: {
    type: String,
    required: true,
  },
  description: {
    type: String,
  },
  value: {
    type: String,
  },
  rows: {
    type: Number,
    default: 5
  }
})
const emit = defineEmits(['updateAddresses'])
const fallthroughAttributes = useSkillsInputFallthroughAttributes()

const value = ref('')
const emailsChanged = () => {
  emit('updateAddresses', value.value);
}
</script>

<template>
  <div>
    <label class="text-secondary" :id="`${field}EmailLabel`" :for="`${field}EmailsInput`">
      {{label}}:
    </label>

    <Textarea :id="`${field}EmailsInput`"
              v-model="value"
              v-bind="fallthroughAttributes.inputAttrs.value"
              @input="emailsChanged"
              class="w-full"
              :rows="rows"
              :aria-labelledby="`${field}EmailLabel`"
              :data-cy="`${field}EmailInput`" />
    <small class="italic">
      {{ description }}
    </small>
  </div>
</template>

<style scoped>

</style>