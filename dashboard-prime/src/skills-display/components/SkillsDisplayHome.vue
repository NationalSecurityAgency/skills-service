<script setup>
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

const displayAttributes = useSkillsDisplayAttributesState()
</script>

<template>
  <div>
    <skills-spinner :is-loading="displayAttributes.loadingConfig" class="mt-8"/>
    <router-view v-if="!displayAttributes.loadingConfig"/>
  </div>
</template>

<style scoped>

</style>