<script setup>
import { useAppVersionState } from '@/stores/UseAppVersionState.js'
import axios from 'axios'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'

const appVersionState = useAppVersionState()
const skillsDisplayInfo = useSkillsDisplayInfo()

const refresh = () => {
  window.location.reload();
}

axios.interceptors.response.use(
  (response) => {
    const incomingVersion = response?.headers?.['skills-client-lib-version'];
    if (incomingVersion) {
      appVersionState.updateLatestLibVersion(incomingVersion)
    }
    return response;
  },
);
</script>

<template>
  <Message v-if="appVersionState.isVersionDifferent" :closable="false" data-cy="newSoftwareVersion" class="mb-3">
    New SkillTree Software Version is Available!! <span v-if="!skillsDisplayInfo.isSkillsClientPath()">Please click <a href="" @click="refresh" data-cy="newSoftwareVersionReload">Here</a>
    to reload.</span><span v-else>Please refresh the page.</span>
  </Message>
</template>

<style scoped>

</style>