<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import SkillsDisplayHome from '@/skills-display/components/SkillsDisplayHome.vue'

const route = useRoute()
const projectId = route.params.projectId
const skillsDisplayAttributes = useSkillsDisplayAttributesState()

onMounted(() => {
  // constructSkillsDisplay()
  skillsDisplayAttributes.projectId = projectId
  skillsDisplayAttributes.serviceUrl = ''
  skillsDisplayAttributes.loadingConfig = false
})
</script>

<template>
<div class="mt-3">
  <skills-display-home />
</div>
</template>

<style scoped>

</style>