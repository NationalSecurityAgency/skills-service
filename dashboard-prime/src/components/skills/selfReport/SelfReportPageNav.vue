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
      <SkillsButton v-if="!isSelfReportConfigureRoute"
                    :to="{ name: 'SelfReportConfigure' }"
                    size="small"
                    data-cy="confApprovalWorkloadConfBtn"
                    icon="fas fa-cogs"
                    label="Configure">
        <i class="fas fa-cogs" aria-hidden="true"/> Configure
      </SkillsButton>
      <SkillsButton v-if="isSelfReportConfigureRoute"
                    :to="{ name: 'SelfReport' }"
                    size="small"
                    icon="fas fa-arrow-alt-circle-left"
                    label="Back" />
    </sub-page-header>

    <div style="width: 99%;">
      <router-view></router-view>
    </div>
  </div>
</template>

<style scoped></style>