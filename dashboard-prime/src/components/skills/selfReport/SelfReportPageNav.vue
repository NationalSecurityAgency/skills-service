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
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";

const route = useRoute();

const isSelfReportConfigureRoute = computed(() => {
  return route.name === 'SelfReportConfigure';
});
</script>

<template>
  <div>
    <sub-page-header title="Self Report">
      <router-link :to="{ name: 'SelfReportConfigure' }" v-if="!isSelfReportConfigureRoute">
        <SkillsButton size="small" icon="fas fa-cogs" label="Configure" data-cy="confApprovalWorkloadConfBtn" />
      </router-link>
      <router-link :to="{ name: 'SelfReport' }" v-if="isSelfReportConfigureRoute">
        <SkillsButton size="small" icon="fas fa-arrow-alt-circle-left" label="Back" />
      </router-link>
    </sub-page-header>

    <div style="width: 99%;">
      <router-view></router-view>
    </div>
  </div>
</template>

<style scoped></style>