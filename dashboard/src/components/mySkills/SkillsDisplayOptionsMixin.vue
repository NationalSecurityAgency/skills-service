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

  </div>
</template>

<script>
  export default {
    name: 'SkillsDisplayMixin',
    computed: {
      options() {
        return {
          projectId: this.projectId,
          authenticator: this.authenticator,
          serviceUrl: this.serviceUrl,
          autoScrollStrategy: 'top-of-page',
        };
      },
      serviceUrl() {
        return window.location.origin;
      },
      authenticator() {
        if (this.$store.getters.isPkiAuthenticated) {
          return 'pki';
        }
        return `${this.serviceUrl}/api/projects/${encodeURIComponent(this.projectId)}/token`;
      },
      userId() {
        let id = null;
        if (this.$store.getters.userInfo) {
          id = this.$store.getters.userInfo.userId;
        }
        return id;
      },
    },
  };
</script>

<style scoped>

</style>
