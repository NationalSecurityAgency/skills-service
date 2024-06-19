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