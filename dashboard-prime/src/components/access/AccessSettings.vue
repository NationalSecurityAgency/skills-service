<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router'
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import RoleManager from "@/components/access/RoleManager.vue";
import PrivateInviteOnlyProjManagement from '@/components/access/invite-only/PrivateInviteOnlyProjManagement.vue'
import SettingsService from '@/components/settings/SettingsService.js'

const route = useRoute();

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

    <skills-spinner v-if="isLoading" :is-loading="isLoading" class="mt-8"/>
    <div v-if="!isLoading">
      <role-manager :project-id="route.params.projectId"
                    title="Project Management Users"
                    data-cy="projectAdmins"
                    :add-role-confirmation="privateProject" />

      <private-invite-only-proj-management />
    </div>
  </div>
</template>

<style scoped></style>
