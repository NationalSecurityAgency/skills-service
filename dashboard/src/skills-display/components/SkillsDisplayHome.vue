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
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useRoute } from 'vue-router'

const displayAttributes = useSkillsDisplayAttributesState()
const route = useRoute()
</script>

<template>
  <div class="sd-theme-home" data-cy="skillsDisplayHome">
    <skills-spinner :is-loading="displayAttributes.loadingConfig" class="mt-8"/>
    <router-view v-if="!displayAttributes.loadingConfig" :key="route.name"/>
  </div>
</template>

<style scoped>

</style>