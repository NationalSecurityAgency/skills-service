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
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import RoleManager from "@/components/access/RoleManager.vue";
import PrivateInviteOnlyProjManagement from '@/components/access/invite-only/PrivateInviteOnlyProjManagement.vue'
import SettingsService from '@/components/settings/SettingsService.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import TrustedClientProps from '@/components/access/TrustedClientProps.vue'

const route = useRoute();
const appConfig = useAppConfig()

const showTrustedClientProps = computed(() => (!appConfig.isPkiAuthenticated));

const isLoading = ref(true);
const privateProject = ref(false);
const userCommunityRestrictedSetting = ref({
  value: false,
  setting: 'user_community',
  projectId: route.params.projectId,
});

const loadData =() => {
  SettingsService.getSettingsForProject(route.params.projectId)
    .then((settingsResponse) => {
      privateProject.value = settingsResponse.find((setting) => setting.setting === 'invite_only')?.enabled;
      userCommunityRestrictedSetting.value.value = Boolean(settingsResponse.find((setting) => setting.setting === 'user_community')?.enabled);
    })
    .finally(() => {
      isLoading.value = false;
    });
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <div>
    <sub-page-header title="Access Management" />

    <skills-spinner v-if="isLoading" :is-loading="isLoading" class="mt-20"/>
    <div v-if="!isLoading">
      <role-manager :project-id="route.params.projectId"
                    title="Project Management Users"
                    data-cy="projectAdmins"
                    :add-role-confirmation="privateProject" />

      <private-invite-only-proj-management v-if="privateProject"/>

      <trusted-client-props v-if="showTrustedClientProps" class="my-6"/>
    </div>
  </div>
</template>

<style scoped></style>
