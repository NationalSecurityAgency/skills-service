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
import { onMounted, ref } from 'vue'
import { SkillsLevelJS, SkillsConfiguration } from '@skilltree/skills-client-js'

const isConfigurationInitialized = ref(false)

onMounted(() => {
  SkillsConfiguration.afterConfigure()
    .then(() => {
      const skillsLevel = new SkillsLevelJS('Inception')
      if (document.querySelector('#skills-level-container')) {
        skillsLevel.attachTo(document.querySelector('#skills-level-container'))
      }

    }).finally(() => {
    isConfigurationInitialized.value = true
  })
})

</script>

<template>
  <router-link to="/administrator/skills/Inception" aria-label="Dashboard Skills" tabindex="-1">
    <Button v-show="isConfigurationInitialized"
            outlined
            icon="fas fa-trophy"
            label="Level 1"
            severity="info">
      <i class="fas fa-trophy mr-1" aria-hidden="true"></i>
      <span id="skills-level-container" />
    </Button>
  </router-link>
</template>

<style scoped>

</style>