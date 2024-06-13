<script setup>
import { useAppInfoState } from '@/stores/UseAppInfoState.js'

const appInfo = useAppInfoState()
</script>

<template>
  <Message v-if="!appInfo.emailEnabled" severity="warn" icon="fa fa-exclamation-triangle" data-cy="emailServiceWarning" :closable="false">
    Please note that email notifications are currently disabled. Email configuration has not been performed on this instance of SkillTree. Please contact the root administrator.
  </Message>
</template>

<style scoped>

</style>