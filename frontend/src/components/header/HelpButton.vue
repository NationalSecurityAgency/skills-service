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
  <b-dropdown right variant="outline-info" :no-caret="true">
    <template slot="button-content">
      <i class="fas fa-question"/>
    </template>
    <b-dropdown-item :href="dashboardGuideUrl" target="_blank" rel="noopener">
      <span class="text-info"> <i class="fas fa-info-circle" style="width: 1.5rem;"/>Dashboard Guide <i class="fas fa-external-link-alt" style="font-size: 0.8rem;"></i></span>
    </b-dropdown-item>
    <b-dropdown-item :href="integrationGuideUrl" target="_blank" rel="noopener">
      <span class="text-info"> <i class="fas fa-hands-helping" style="width: 1.5rem;"></i>Integration Guide <i class="fas fa-external-link-alt" style="font-size: 0.8rem;"></i></span>
    </b-dropdown-item>
  </b-dropdown>
</template>

<script>
  export default {
    name: 'HelpButton',
    computed: {
      dashboardGuideUrl() {
        return `${this.$store.getters.config.docsHost}/dashboard/user-guide/`;
      },
      integrationGuideUrl() {
        return `${this.$store.getters.config.docsHost}/skills-client/`;
      },
    },
  };
</script>

<style scoped>


</style>
