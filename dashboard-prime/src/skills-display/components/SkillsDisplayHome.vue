<script setup>
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useRoute } from 'vue-router'

const displayAttributes = useSkillsDisplayAttributesState()
const route = useRoute()
</script>

<template>
  <div class="sd-theme-home">
    <skills-spinner :is-loading="displayAttributes.loadingConfig" class="mt-8"/>
    <router-view v-if="!displayAttributes.loadingConfig" :key="route.path"/>
  </div>
</template>

<style scoped>

</style>