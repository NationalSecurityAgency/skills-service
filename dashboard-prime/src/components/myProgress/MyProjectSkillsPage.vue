<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import SkillsDisplayHome from '@/skills-display/components/SkillsDisplayHome.vue'
import ProjectService from '@/components/projects/ProjectService.js'

const route = useRoute()
const projectId = route.params.projectId
const skillsDisplayAttributes = useSkillsDisplayAttributesState()

onMounted(() => {
  // constructSkillsDisplay()
  skillsDisplayAttributes.projectId = projectId
  skillsDisplayAttributes.serviceUrl = ''
  skillsDisplayAttributes.loadingConfig = false
  handleProjInvitation()
})

const handleProjInvitation = () => {
  const isInvited = route.query.invited;
  if (isInvited) {
    ProjectService.addToMyProjects(projectId);
  }
}
</script>

<template>
  <div>
    <skills-display-home :id="projectId" class="my-3" />
  </div>
</template>

<style scoped>

</style>