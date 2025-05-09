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
import { ref, computed } from 'vue'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'
import { useRoute } from 'vue-router'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import ContactProjectAdminsDialog from "@/components/contact/ContactProjectAdminsDialog.vue";

const appInfo = useAppInfoState()
const route = useRoute()
const colors = useColors()

const showContact = ref(false)

const projectId = computed(() => route.params.projectId)
</script>

<template>
  <div class="my-20">
    <div class="flex justify-center">
      <div class="rounded-lg w-24 h-24 m-2 bg-red-400 font-bold flex items-center justify-center">
        <i class="text-surface-0 dark:text-surface-900 text-7xl fas fa-shield-alt" aria-hidden="true"></i>
      </div>
    </div>
    <div class="text-center">
      <h1 class="text-2xl text-orange-800 dark:text-orange-400">Restricted Access</h1>
    </div>

    <div class="max-w-md lg:max-w-xl mx-auto text-center mt-4" data-cy="notAuthorizedExplanation">
      <Card>
        <template #content>
          <p class="mb-5">
            Access to this training is currently restricted. If you're interested in participating, please reach out to the project administrators to request an invitation.
          </p>
          <SkillsButton
            v-if="appInfo.emailEnabled"
            label="Contact Administrators"
            icon="fas fa-mail-bulk"
            @click="showContact=true"
            data-cy="contactOwnerBtn" />
        </template>
      </Card>
    </div>
    <contact-project-admins-dialog v-if="showContact"
                           v-model="showContact"
                           :project-id="projectId" />
  </div>
</template>

<style scoped>

</style>