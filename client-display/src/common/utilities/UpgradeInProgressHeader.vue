/*
Copyright 2020 SkillTree

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
<template>
  <div v-if="upgradeInProgress" data-cy="upgradeInProgressHeader">
    <span class="fa-stack fa-2x" style="vertical-align: middle; font-size:1em;">
        <i class="fas fa-circle fa-stack-2x"></i>
        <i class="fas fa-hammer fa-stack-1x fa-inverse"></i>
      </span>
    <span class="pl-1">An upgrade is currently in process. Please note that any reported skills will be queued until the upgrade has completed, as such your profile may not reflect your most recent skill achievements.</span>
  </div>
</template>

<script>
  export default {
    name: 'UpgradeInProgressHeader',
    computed: {
      upgradeInProgress() {
        return this.$store.getters.config && this.$store.getters.config.dbUpgradeInProgress && this.$store.getters.config.dbUpgradeInProgress === 'true';
      },
    },
  };
</script>