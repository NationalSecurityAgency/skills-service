/*
Copyright 2025 SkillTree

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

import Select from "primevue/select";
import {computed, onMounted, ref, watch} from "vue";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";

const emits = defineEmits(['add-prefix', 'preview-prefix'])
const props  = defineProps({
  id: {
    type: String,
    default: 'prefix-controls-default',
  },
  showPreviewControl: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'small'
  },
  outlinedButtons: {
    type: Boolean,
    default: true
  },
  buttonsSeverity: {
    type: String,
    default: 'success'
  },
  addButtonLabelConfProp: {
    type: String,
    default: 'addPrefixToInvalidParagraphsBtnLabel'
  },
  isLoading: {
    type: Boolean,
    default: false
  },
  communityValue: {
    type: String,
    default: null
  },
})
const appConfig = useAppConfig()

const supportPrefix = computed(() => appConfig.paragraphValidationRegex && prefixOptions.value.length > 0 )
const prefixOptions = ref ([])
const prefix = ref('')

onMounted(() => {
  updatedPrefixOptions()
})

watch(() => props.communityValue, () => {
  updatedPrefixOptions()
})

const updatedPrefixOptions = () => {
  const prefixOptionsPropVal = appConfig.addPrefixToInvalidParagraphsOptions
  if (prefixOptionsPropVal && prefixOptionsPropVal.length > 0) {
    let pOptions
    if (prefixOptionsPropVal.includes(':')) {
      const parsedOptionsLookup = parseAddPrefixToInvalidParagraphsOptions(prefixOptionsPropVal)
      console.log(`parsedOptionsLookup: ${JSON.stringify(parsedOptionsLookup)}, communityValue: ${props.communityValue}`)
      pOptions = parsedOptionsLookup[props.communityValue]
    } else {
      pOptions = prefixOptionsPropVal.split(',');
    }
    if (pOptions && pOptions.length > 0) {
      prefixOptions.value = pOptions
      prefix.value = pOptions[0]
    }
  }
}


const parseAddPrefixToInvalidParagraphsOptions = (str) => {
  const result = {};
  // Split by '|' to separate different sections
  const sections = str.split('|');

  sections.forEach(section => {
    // Split each section into key and values
    const [key, valuesStr] = section.split(':');
    if (key && valuesStr) {
      // Trim whitespace and split values by comma
      const values = valuesStr.split(',')
      result[key.trim()] = valuesStr.split(',');
    }
  });

  return result;
}

const addPrefix = () => {
  emits('add-prefix', {id: props.id, prefix: prefix.value})
}
const previewPrefix = () => {
  emits('preview-prefix', {id: props.id, prefix: prefix.value})
}
</script>

<template>
  <div v-if="supportPrefix" class="flex gap-2">
    <Select v-model="prefix"
            :options="prefixOptions"
            name="prefix"
            data-cy="prefixSelect"
            :size="size"
            :disabled="isLoading"/>
    <SkillsButton icon="fa-solid fa-hammer"
                  :label="appConfig[addButtonLabelConfProp]"
                  :size="size"
                  :outlined="outlinedButtons"
                  :severity="buttonsSeverity"
                  :loading="isLoading"
                  data-cy="addPrefixBtn"
                  @click="addPrefix"/>
    <SkillsButton v-if="showPreviewControl"
                  icon="fa-solid fa-magnifying-glass"
                  :label="appConfig.showMissingPrefixBtnLabel"
                  :size="size"
                  :outlined="outlinedButtons"
                  :severity="buttonsSeverity"
                  :loading="isLoading"
                  data-cy="previewPrefixBtn"
                  @click="previewPrefix"/>

  </div>
</template>

<style scoped>

</style>