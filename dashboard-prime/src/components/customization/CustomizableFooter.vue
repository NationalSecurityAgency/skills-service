<script setup>
import { computed } from 'vue';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useDynamicVariableReplacementHelper } from '@/components/customization/DynamicVariableReplacementHelper.js';

const appConfig = useAppConfig();
const variableReplacementHelper = useDynamicVariableReplacementHelper();

let customFooter = computed(() => {
  return variableReplacementHelper.populateDynamicVariables(appConfig.customFooter)
})
</script>

<template>
  <div v-html="customFooter">
  </div>
</template>

<style scoped>

</style>