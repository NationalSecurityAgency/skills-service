<script setup>
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import EmailServerSettings from "@/components/settings/EmailServerSettings.vue";
import EmailTemplateSettings from "@/components/settings/EmailTemplateSettings.vue";
</script>

<template>
  <sub-page-header title="Email Settings"/>
  <Card class="mb-4">
    <template #header>
      <SkillsCardHeader title="Email Connection Settings"></SkillsCardHeader>
    </template>
    <template #content>
      <email-server-settings />
    </template>
  </Card>

  <Card>
    <template #header>
      <SkillsCardHeader title="Email Template Settings"></SkillsCardHeader>
    </template>
    <template #content>
      <email-template-settings />
    </template>
  </Card>
</template>

<style scoped></style>
