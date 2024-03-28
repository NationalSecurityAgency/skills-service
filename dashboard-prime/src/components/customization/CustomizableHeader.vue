<script setup>
import { computed } from 'vue';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useDynamicVariableReplacementHelper } from '@/components/customization/DynamicVariableReplacementHelper.js';

const appConfig = useAppConfig();
const variableReplacementHelper = useDynamicVariableReplacementHelper();

let customHeader = computed(() => {
  return variableReplacementHelper.populateDynamicVariables(appConfig.customHeader)
})
</script>

<template>
  <div v-html="customHeader">
  </div>
</template>

<style scoped>

</style>