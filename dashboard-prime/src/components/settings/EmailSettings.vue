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
