<script setup>

import UsersTable from "@/components/users/UsersTable.vue";

defineProps({
  title: {
    type: String,
    required: false,
    default: 'Users',
  }
});
</script>

<template>
  <Card :no-padding="true" data-cy="usersTableMetric" >
    <template #header>
      <SkillsCardHeader :title="title"></SkillsCardHeader>
    </template>
    <template #content>
      <users-table />
    </template>
  </Card>
</template>

<style scoped>

</style>