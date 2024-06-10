<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import SkillsDisplayHome from '@/skills-display/components/SkillsDisplayHome.vue'
import ProjectService from '@/components/projects/ProjectService.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'

const route = useRoute()
const projectId = route.params.projectId
const skillsDisplayAttributes = useSkillsDisplayAttributesState()
const themeState = useSkillsDisplayThemeState()

themeState.theme.disableSkillTreeBrand = true
themeState.theme.disableBreadcrumb = true
themeState.theme.pageTitle = {
  textAlign: 'left',
  fontSize: '1.5rem'
}

onMounted(() => {
  skillsDisplayAttributes.projectId = projectId
  skillsDisplayAttributes.serviceUrl = ''
  skillsDisplayAttributes.loadingConfig = false
  skillsDisplayAttributes.internalBackButton = false

  themeState.theme.landingPageTitle = `${skillsDisplayAttributes.projectDisplayName}: ${skillsDisplayAttributes.projectName}`
  themeState.initThemeObjInStyleTag(themeState.theme)
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