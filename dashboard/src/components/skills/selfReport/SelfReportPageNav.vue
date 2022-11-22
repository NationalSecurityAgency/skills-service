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
  <div class="mb-3">
    <sub-page-header title="Self Report">
      <b-button v-if="!isSelfReportConfigureRoute" :to="{ name: 'SelfReportConfigure' }" variant="outline-primary" size="sm" data-cy="confApprovalWorkloadConfBtn">
        <i class="fas fa-cogs" aria-hidden="true"/> Configure
      </b-button>
      <b-button v-if="isSelfReportConfigureRoute" :to="{ name: 'SelfReport' }" variant="outline-primary" size="sm">
        <i class="fas fa-arrow-alt-circle-left" aria-hidden="true"/> Back
      </b-button>
    </sub-page-header>

    <!-- on FF charts end up pushing column to the next row; this is a workaround -->
    <div style="width: 99%;">
      <router-view></router-view>
    </div>
  </div>
</template>

<script>
  import SubPageHeader from '@//components/utils/pages/SubPageHeader';

  export default {
    name: 'MetricsPageNav',
    components: { SubPageHeader },
    data() {
      return {
      };
    },
    computed: {
      isSelfReportConfigureRoute() {
        return this.$route.name === 'SelfReportConfigure';
      },
    },
  };

</script>

<style scoped>

</style>
