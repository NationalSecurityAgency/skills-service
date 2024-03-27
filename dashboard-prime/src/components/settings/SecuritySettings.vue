<script setup>
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
</script>

<template>
  <div>
    <sub-page-header title="Security Settings"/>

    <Card>
      <template #header>
        <SkillsCardHeader title="Root Users Management"></SkillsCardHeader>
      </template>
      <template #content></template>
    </Card>

    <Card>
      <template #header>
        <SkillsCardHeader title="Supervisor Users Management"></SkillsCardHeader>
      </template>
      <template #content></template>
    </Card>
  </div>
</template>

<style scoped></style>
