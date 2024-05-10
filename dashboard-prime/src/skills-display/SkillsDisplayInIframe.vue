<script setup>
import { ref } from 'vue'
import SkillsDisplayHome from '@/skills-display/components/SkillsDisplayHome.vue'

const appStyleObject = ref({})
</script>

<template>
  <div
    id="skills-display-app"
    class="skills-client-app"
    ref="content"
    tabindex="-1"
    role="main"
    :style="appStyleObject"
    aria-label="SkillTree Skills Display">
    <skills-display-home />
  </div>
</template>

<style scoped>

</style>