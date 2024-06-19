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