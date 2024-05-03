<script setup>
import { ref } from 'vue';
import { useRoute } from 'vue-router'
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import RoleManager from "@/components/access/RoleManager.vue";

const route = useRoute();

const isLoading = ref(true);
const privateProject = ref(false);
const userCommunityRestrictedSetting = ref({
  value: false,
  setting: 'user_community',
  projectId: route.params.projectId,
});
const errMsg = ref(null);
</script>

<template>
  <div>
    <sub-page-header title="Access Management"/>

    <Card data-cy="projectAdmins">
      <template #header>
        <SkillsCardHeader title="Project Management Users"></SkillsCardHeader>
      </template>
      <template #content>
        <role-manager :project-id="route.params.projectId" :add-role-confirmation="privateProject"/>
      </template>
    </Card>
  </div>
</template>

<style scoped></style>
