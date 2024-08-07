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
import { onMounted, ref, computed } from 'vue'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import UsersService from '@/components/users/UsersService.js'
import SkillsDisplayHome from '@/skills-display/components/SkillsDisplayHome.vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useRoute } from 'vue-router'
import { useAuthState } from '@/stores/UseAuthState.js'

const projConfig = useProjConfig()

const skillsDisplayAttributes = useSkillsDisplayAttributesState()
const themeState = useSkillsDisplayThemeState()
const authState = useAuthState()
const route = useRoute()

skillsDisplayAttributes.projectId = route.params.projectId
skillsDisplayAttributes.userId = route.params.userId
skillsDisplayAttributes.serviceUrl = ''
skillsDisplayAttributes.loadingConfig = false
skillsDisplayAttributes.internalBackButton = false

themeState.theme.landingPageTitle = 'User\'s Skills Preview'
themeState.theme.disableSkillTreeBrand = true
themeState.theme.disableBreadcrumb = true
skillsDisplayAttributes.loadConfigStateIfNeeded()

const checkingAccess = ref(true)
const canAccess = ref(true)

onMounted(() => {
  projConfig.afterProjConfigStateLoaded().then((projConfigRes) => {
    if (projConfigRes.invite_only === 'true') {
      canAccess.value = false;
      const userId = authState.userInfo?.dn || route.params.userId
      UsersService.canAccess(route.params.projectId, userId).then((res) => {
        canAccess.value = res === true;
      }).finally(() => {
        checkingAccess.value = false;
      });
    } else {
      checkingAccess.value = false;
    }
  })
})

const isLoading = computed(() => checkingAccess.value || skillsDisplayAttributes.loadingConfig)
</script>

<template>
<div>
  <sub-page-header title="User's Display">
  </sub-page-header>

  <skills-spinner :is-loading="isLoading" />
  <div v-if="!isLoading">
    <Message v-if="!canAccess" icon="fas fa-user-slash" severity="error" :closable="false">
      Access Revoked! This user's access was previously revoked, their Client Display is disabled until they are granted access.
    </Message>
    <skills-display-home v-if="canAccess" class="my-3" />
  </div>
</div>
</template>

<style scoped>

</style>