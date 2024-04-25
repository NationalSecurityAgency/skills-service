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