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
import { useField } from 'vee-validate'
import { useSkillsInputFallthroughAttributes } from "@/components/utils/inputForm/UseSkillsInputFallthroughAttributes.js";
const props = defineProps({
  name: {
    type: String,
    required: true
  },
})
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