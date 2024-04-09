<script setup>
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'

const skillsDisplayTheme = useSkillsDisplayThemeState()
</script>

<template>
  <div>
    <Card>
      <template #content>
        <div class="text-center text-2xl uppercase text-primary">
        {{ skillsDisplayTheme.landingPageTitle }}
        </div>
      </template>
    </Card>

    <Card class="mt-3">
      <template #content>
        <div class="flex text-center">
          <div class="flex-1">
            Overall Points
          </div>
          <div class="flex-1">
            My Level
          </div>
          <div class="flex-1">
            Level Progress
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>