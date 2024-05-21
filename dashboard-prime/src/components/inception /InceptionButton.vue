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
  <router-link to="/administrator/skills/Inception" aria-label="Dashboard Skills">
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