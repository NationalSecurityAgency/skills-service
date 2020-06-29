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
  <b-alert
    v-model="showNewVersionAlert"
    class="position-fixed fixed-top m-0 rounded-0"
    style="z-index: 2000;"
    variant="success"
    dismissible
  >
    New Software Version is Available!! Please click <a href="" @click="refresh">Here</a>
    to reload.
  </b-alert>
</template>

<script>
  export default {
    name: 'NewSoftwareVersionComponent',
    data() {
      return {
        showNewVersionAlert: false,
        currentLibVersion: undefined,
      };
    },
    mounted() {
      this.updateStorageIfNeeded();
    },
    computed: {
      libVersion() {
        return this.$store.getters.libVersion;
      },
    },
    watch: {
      libVersion() {
        if (localStorage.skillsDashboardLibVersion !== undefined
          && this.libVersion !== undefined
          && this.libVersion.localeCompare(localStorage.skillsDashboardLibVersion) > 0) {
          this.showNewVersionAlert = true;
        }
        this.updateStorageIfNeeded();
      },
    },
    methods: {
      refresh() {
        window.location.reload();
      },
      updateStorageIfNeeded() {
        const storedVal = localStorage.skillsDashboardLibVersion;
        const currentVersion = this.libVersion;
        if (currentVersion !== undefined && (storedVal === undefined || currentVersion.localeCompare(storedVal) > 0)) {
          localStorage.skillsDashboardLibVersion = currentVersion;
        }
      },
    },
  };
</script>

<style scoped>

</style>
