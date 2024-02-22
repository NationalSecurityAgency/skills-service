<script setup>
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import UsersTable from './UsersTable.vue';
</script>

<template>
  <div class="usersTable">
    <sub-page-header title="Users"/>
    <Card>
      <template #content>
        <UsersTable />
      </template>
    </Card>
  </div>
</template>

<style scoped></style>
