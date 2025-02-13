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
import { computed } from 'vue';
import InputGroup from 'primevue/inputgroup';
import InputGroupAddon from 'primevue/inputgroupaddon';
import {useField} from "vee-validate";
import { useProjConfig } from '@/stores/UseProjConfig.js'

const props = defineProps({
  name: String,
  nextFocusEl: HTMLElement,
});
const config = useProjConfig();
const { value, errorMessage } = useField(() => props.name);

const projConfigRootHelpUrl = config.projConfigRootHelpUrl;

const overrideRootHelpUrl = computed(() => {
  return projConfigRootHelpUrl && value.value && (value.value.startsWith('http://') || value.value.startsWith('https://'));
});

const rootHelpUrl = computed(() => {
  if (!projConfigRootHelpUrl) {
    return projConfigRootHelpUrl;
  }
  if (projConfigRootHelpUrl.endsWith('/')) {
    return projConfigRootHelpUrl.substring(0, projConfigRootHelpUrl.length - 1);
  }
  return projConfigRootHelpUrl;
});

</script>

<template>
  <div class="field">
    <label for="skillHelpUrl">Help URL/Path</label>

    <InputGroup>
      <InputGroupAddon v-if="projConfigRootHelpUrl">
        <i class="fas fa-cogs mr-1" aria-hidden="true"></i>
        <span class="text-primary"
              :class="{ 'line-through' : overrideRootHelpUrl}"
              data-cy="rootHelpUrlSetting"
              id="rootHelpUrlHelp"
              :aria-label="`Root Help URL was configured in the project's settings. Root Help URL is ${rootHelpUrl}. URLs starting with http or https will not use Root Help URL.`"
              >{{ rootHelpUrl }}</span>
      </InputGroupAddon>
      <InputText v-model="value" data-cy="skillHelpUrl" id="skillHelpUrl"></InputText>
    </InputGroup>
    <Message severity="error"
             variant="simple"
             size="small"
             :closable="false"
             data-cy="skillHelpUrlError"
             :id="`${name}Error`">{{ errorMessage || '' }}</Message>
  </div>
</template>

<style scoped></style>
