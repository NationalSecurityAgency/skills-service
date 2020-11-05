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
  <b-dropdown right variant="link" aria-label="Help Menu Dropdown" id="helpMenu">
    <template slot="button-content">
      <i class="far fa-question-circle" aria-label="help menu button"></i><span class="sr-only">help menu button</span>
    </template>
    <b-dropdown-item :href="officialGuide" target="_blank" style="min-width: 12.5rem;">
      <span class="text-gray-700"> <i class="fas fa-book" aria-hidden="true"></i>Official Docs</span>
      <span class="float-right" aria-hidden="true"><i class="fas fa-external-link-alt text-secondary"></i></span>
    </b-dropdown-item>
    <b-dropdown-divider />
    <b-dropdown-group id="dropdown-group-1" header="Guides">
      <b-dropdown-item :href="dashboardGuideUrl" target="_blank">
        <span class="text-gray-700"> <i class="fas fa-info-circle" aria-hidden="true"/><span class="link-name">Dashboard</span></span>
        <span class="float-right" aria-hidden="true"><i class="fas fa-external-link-alt text-secondary"></i></span>
      </b-dropdown-item>
      <b-dropdown-item :href="integrationGuideUrl" target="_blank">
        <span class="text-gray-700"> <i class="fas fa-hands-helping" aria-hidden="true"></i>Integration</span>
        <span class="float-right" aria-hidden="true"><i class="fas fa-external-link-alt text-secondary"></i></span>
      </b-dropdown-item>
    </b-dropdown-group>
  </b-dropdown>
</template>

<script>
  export default {
    name: 'HelpButton',
    computed: {
      officialGuide() {
        return `${this.$store.getters.config.docsHost}`;
      },
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
.fa-external-link-alt {
  font-size: 0.6rem;
}
.link-name {
  padding-right: 2rem;
}
.text-gray-700 {
  width: 40rem !important;
}
.text-gray-700 > i {
  width: 1.6rem;
}
.sr-only {
  position:absolute;
  left:-10000px;
  top:auto;
  width:1px;
  height:1px;
  overflow:hidden;
}
</style>
