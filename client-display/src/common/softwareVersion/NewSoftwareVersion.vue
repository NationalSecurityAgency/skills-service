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
    <div>
        <div v-if="showNewVersionAlert" class="mt-2 mb-3 card-body skills-page-title-text-color card rounded bg-white text-info">
            <h5>
                <i class="fas fa-exclamation-circle"></i> New Skills Software Version is Available!! Please refresh the page.
            </h5>
        </div>
    </div>
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
        return this.$store.state.softwareVersion;
      },
    },
    watch: {
      libVersion() {
        if (this.currentLibVersion !== undefined
          && this.libVersion !== undefined
          && this.libVersion.localeCompare(this.currentLibVersion) > 0) {
          this.showNewVersionAlert = true;
        }
        this.updateStorageIfNeeded();
      },
    },
    methods: {
      updateStorageIfNeeded() {
        const storedVal = this.currentLibVersion;
        const currentVersion = this.libVersion;
        if (currentVersion !== undefined && (storedVal === undefined || currentVersion.localeCompare(storedVal) > 0)) {
          this.currentLibVersion = currentVersion;
        }
      },
    },
  };
</script>

<style scoped>
</style>
