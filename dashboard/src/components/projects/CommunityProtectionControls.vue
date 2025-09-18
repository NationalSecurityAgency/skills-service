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
import { useAuthState } from '@/stores/UseAuthState.js'
import { computed, onMounted, ref } from 'vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useCommunityLabels } from '@/components/utils/UseCommunityLabels.js'
import Avatar from 'primevue/avatar'
import {useField, useIsValidating} from 'vee-validate'
import { syncRef } from '@vueuse/core'
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";

const props = defineProps({
  project: {
    type: Object,
    default: null,
  },
  isCopy: {
    type: Boolean,
    default: false
  },
  isEdit: {
    type: Boolean,
    default: false
  },
  adminGroup: {
    type: Object,
    default: null
  },
  quiz: {
    type: Object,
    default: null
  },
  globalBadge: {
    type: Object,
    default: null
  },
})
if (!props.project && !props.adminGroup && !props.quiz && !props.globalBadge) {
  throw new Error('Either project, adminGroup, uiz  or globalBadge must be provided')
}
const authState = useAuthState()
const appConfig = useAppConfig()
const communityLabels = useCommunityLabels()

const isValidating = useIsValidating()
const { value, errors } = useField('enableProtectedUserCommunity')

const initialValueForEnableProtectedUserCommunity = ref(null)
const enableProtectedUserCommunity = defineModel('enableProtectedUserCommunity')
const enableProtectedUserCommunitySynced = syncRef(enableProtectedUserCommunity, value)
const typeLabel = computed(() => {
  if(props.project) {
    return 'a project'
  } else if (props.quiz) {
    return 'a quiz'
  } else if (props.adminGroup) {
    return 'an admin group'
  } else if (props.globalBadge) {
    return 'a global badge'
  }
})

onMounted(() => {
  initialValueForEnableProtectedUserCommunity.value = communityLabels.isRestrictedUserCommunity(props.project?.userCommunity)
      || communityLabels.isRestrictedUserCommunity(props.adminGroup?.userCommunity)
      || communityLabels.isRestrictedUserCommunity(props.quiz?.userCommunity)
      || communityLabels.isRestrictedUserCommunity(props.globalBadge?.userCommunity)
  enableProtectedUserCommunitySynced.value = initialValueForEnableProtectedUserCommunity.value

  // enableProtectedUserCommunity.value = this.isRestrictedUserCommunity(this.project.userCommunity);
  // initialValueForEnableProtectedUserCommunity.value = this.internalProject.enableProtectedUserCommunity;
  // if (this.isCopy && this.initialValueForEnableProtectedUserCommunity) {
  //   this.originalProject.enableProtectedUserCommunity = this.initialValueForEnableProtectedUserCommunity;
  // }
})

const isCopyAndCommunityProtected = computed(() => {
  return props.isCopy && initialValueForEnableProtectedUserCommunity.value
})
const isEditAndCommunityProtected = computed(() => {
  return props.isEdit && initialValueForEnableProtectedUserCommunity.value
})
const userCommunityRestrictedDescriptor = computed(() => {
  return appConfig.userCommunityRestrictedDescriptor
})


</script>

<template>
  <Card v-if="authState.showUserCommunityInfo"
        :pt="{ body: { class: 'p-0' }, content: { class: 'py-3 px-3' } }"
        data-cy="restrictCommunityControls">
    <template #content>
      <div v-if="isCopyAndCommunityProtected" severity="error" :closable="false" data-cy="protectedCopyMessage">
        <Avatar icon="fas fa-shield-alt" class="text-red-500"></Avatar>
        Copying {{typeLabel}} whose access is restricted to <b
        class="text-primary">{{ userCommunityRestrictedDescriptor }}</b> users only and <b>cannot</b> be lifted/disabled
      </div>
      <div v-if="isEditAndCommunityProtected" severity="error" :closable="false">
        <Avatar icon="fas fa-shield-alt" class="text-red-500"></Avatar>
        Access is restricted to <b class="text-primary">{{ userCommunityRestrictedDescriptor
        }}</b> users only and <b>cannot</b> be lifted/disabled
      </div>
      <div v-if="!isEditAndCommunityProtected && !isCopyAndCommunityProtected">
        <div class="flex items-center content-center">
          <div class="flex-1 flex items-center content-center">

            <ToggleSwitch
              data-cy="restrictCommunity"
              :aria-label="`Restrict Access to ${userCommunityRestrictedDescriptor} Users Only`"
              class="mr-2"
              v-model="value" />
            <label id="restrictMsg">Restrict <i class="fas fa-shield-alt text-red-500" aria-hidden="true" /> Access to
              <b
                class="text-primary">{{ userCommunityRestrictedDescriptor }}</b> users only
            </label>
          </div>
          <div v-if="appConfig.userCommunityDocsLink" class="col-lg-auto" data-cy="userCommunityDocsLink">
            <a :href="appConfig.userCommunityDocsLink" target="_blank"
               style="text-decoration: underline">{{ appConfig.userCommunityDocsLabel }}</a>
            <i class="fas fa-external-link-alt ml-1" aria-hidden="true" style="font-size: 0.9rem;" />
          </div>
        </div>
        <SkillsSpinner v-if="isValidating && enableProtectedUserCommunity" :is-loading="true" class="my-0"/>
        <div v-if="!isValidating">
          <Message v-if="enableProtectedUserCommunity && !(errors && errors.length > 0)"
                   :closable="false"
                   severity="warn"
                   data-cy="communityRestrictionWarning">
            Please note that once the restriction is enabled it <b>cannot</b> be lifted/disabled.
          </Message>
        </div>

        <Message v-if="errors && errors.length > 0"
                 :closable="false"
                 data-cy="communityProtectionErrors"
                 class="mt-2"
                 severity="error">
          <div>Unable to restrict access to {{ userCommunityRestrictedDescriptor }} users only:</div>
          <div v-for="error in errors" :key="error"
               class="p-error ml-2 mt-1">{{ error }}
          </div>
        </Message>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>