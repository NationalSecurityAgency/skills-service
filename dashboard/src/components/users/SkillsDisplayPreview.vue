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
import { computed, onMounted, ref } from 'vue'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import UsersService from '@/components/users/UsersService.js'
import SkillsDisplayHome from '@/skills-display/components/SkillsDisplayHome.vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useRoute, useRouter } from 'vue-router'
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue'
import { useDialogMessages } from '@/components/utils/modal/UseDialogMessages.js'
import { useFocusState } from '@/stores/UseFocusState.js'

const projConfig = useProjConfig()

const skillsDisplayAttributes = useSkillsDisplayAttributesState()
const themeState = useSkillsDisplayThemeState()
const route = useRoute()
const router = useRouter()
const dialogMessages = useDialogMessages()
const focusState = useFocusState()

skillsDisplayAttributes.projectId = route.params.projectId
skillsDisplayAttributes.userId = route.params.userId
skillsDisplayAttributes.serviceUrl = ''
skillsDisplayAttributes.loadingConfig = false
skillsDisplayAttributes.internalBackButton = false

themeState.theme.landingPageTitle = 'User\'s Skills Preview'
themeState.theme.disableSkillTreeBrand = true
themeState.theme.disableBreadcrumb = true
themeState.theme.disableSearchButton = true
themeState.theme.disableEncouragementsConfetti = true
skillsDisplayAttributes.loadConfigStateIfNeeded()

const checkingAccess = ref(true)
const canAccess = ref(true)
const showRemoveDialog = ref(false)
const removingUser = ref(false)
const removalError = ref(null)

const confirmRemoval = () => {
  removalError.value = null
  showRemoveDialog.value = true
}

const removeUser = async () => {
  if (removingUser.value) {
    return
  }
  removingUser.value = true
  removalError.value = null
  const { projectId, userId } = route.params
  try {
    const result = await UsersService.removeUserFromProject(projectId, userId)
    if (!result.success) {
      removalError.value = result.explanation || 'The user could not be removed. Please try again.'
      return
    }
    await router.push({ name: 'ProjectUsers', params: { projectId } })
    focusState.setElementId('mainContent1')
    dialogMessages.msgOk({
      header: 'User Removed',
      message: `User ${userId} was removed from this project.`
    })
  } catch (error) {
    removalError.value = error.response?.data?.explanation || 'The user could not be removed. Please try again.'
  } finally {
    removingUser.value = false
  }
}

onMounted(() => {
  projConfig.afterProjConfigStateLoaded().then((projConfigRes) => {
    if (projConfigRes.invite_only === 'true') {
      canAccess.value = false;
      UsersService.canAccess(route.params.projectId, route.params.userId).then((res) => {
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
  <sub-page-header title="User's Display" :is-loading="isLoading">
    <SkillsButton v-if="!projConfig.isReadOnlyProj"
                  id="removeProjectUserBtn"
                  label="Remove User"
                  icon="fa-solid fa-user-slash"
                  severity="danger"
                  size="small"
                  outlined
                  :disabled="removingUser"
                  :loading="removingUser"
                  :track-for-focus="true"
                  aria-label="Remove this user from the project"
                  data-cy="removeProjectUserBtn"
                  @click="confirmRemoval" />
  </sub-page-header>

  <Message v-if="removalError" severity="error" :closable="false" data-cy="removeProjectUserError">{{ removalError }}</Message>
  <skills-spinner :is-loading="isLoading" />
  <div v-if="!isLoading">
    <Message v-if="!canAccess" icon="fas fa-user-slash" severity="error" :closable="false">
      Access Revoked! This user's access was previously revoked, their Client Display is disabled until they are granted access.
    </Message>
    <skills-display-home v-if="canAccess" preview class="mb-4" />
  </div>
  <RemovalValidation v-if="showRemoveDialog"
                     v-model="showRemoveDialog"
                     :item-name="route.params.userId"
                     item-type="from this project"
                     remove-button-label="Yes, Remove User!"
                     focus-on-close-id="removeProjectUserBtn"
                     @do-remove="removeUser">
    <div class="flex flex-col gap-4 leading-relaxed">
      <p>This action cannot be undone and <strong>permanently deletes</strong> this user's progress in this project, including points and achievements.</p>
      <div>
        <p class="font-semibold mb-2">Additional effects, if applicable:</p>
        <ul class="list-disc pl-5 space-y-3">
          <li data-cy="removeProjectUserQuizWarning">
            <strong>Quizzes:</strong> If this project has skills associated with quizzes, this user's passed attempts for those quizzes will also be removed. Their progress in other projects that use the same quizzes may be affected.
          </li>
          <li data-cy="removeProjectUserCatalogWarning">
            <strong>Skills Catalog:</strong> If this project exports skills to the Skills Catalog, removing this user's progress may also remove their points and achievements in other projects that import those skills.
          </li>
        </ul>
      </div>
      <p data-cy="removeProjectUserAccountWarning"><strong>Not removed:</strong> The user's account, roles, and project access.</p>
    </div>
  </RemovalValidation>
</div>
</template>

<style scoped>

</style>
