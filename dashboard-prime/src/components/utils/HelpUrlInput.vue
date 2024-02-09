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


const handleEscape = () => {
};

</script>

<template>
  <div class="field">
    <label for="skillHelpUrl">Help URL/Path</label>

    <InputGroup>
      <InputGroupAddon v-if="projConfigRootHelpUrl">
        <i class="fas fa-cogs mr-1"></i>
        <span class="text-primary" :class="{ 'line-through' : overrideRootHelpUrl}" data-cy="rootHelpUrlSetting"
              id="rootHelpUrlHelp"
              :aria-label="`Root Help URL was configured in the project's settings. Root Help URL is ${rootHelpUrl}. URLs starting with http or https will not use Root Help URL.`"
              tabindex="0"
              @keydown.esc="handleEscape">{{ rootHelpUrl }}</span>
      </InputGroupAddon>
      <InputText v-model="value" data-cy="skillHelpUrl" id="skillHelpUrl"></InputText>
    </InputGroup>
    <small
        role="alert"
        class="p-error"
        :data-cy="`${name}Error`"
        :id="`${name}Error`">{{ errorMessage || '&nbsp;' }}</small>
  </div>
</template>

<style scoped></style>
