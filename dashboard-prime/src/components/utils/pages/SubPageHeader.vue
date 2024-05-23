<script setup>
import {ref, computed, watch} from 'vue';
import { useProjConfig } from '@/stores/UseProjConfig.js'
import { useRoute } from "vue-router";

const emit = defineEmits(['add-action'])
const props = defineProps(['title', 'action', 'disabled', 'disabledMsg', 'ariaLabel', 'isLoading', 'marginBottom']);
const route = useRoute()

const config = useProjConfig();
const actionButton = ref(null)
const disabledInternal = ref(props.disabled);

const isAdminProjectsPage = computed(() => {
  return route.path?.toLowerCase() === '/administrator/' || route.path?.toLowerCase() === '/administrator';
});

const isAdminPage = computed(() => {
  return route.path?.toLowerCase()?.startsWith('/administrator');
});

const isReadOnlyProj = computed(() => config.isReadOnlyProj);

const isMetricsPage = computed(() => {
  const projId = route.params?.projectId;
  if (!projId) {
    return false;
  }
  const startsWith = `/administrator/projects/${projId}/metrics`;
  return route.path?.toLowerCase()?.startsWith(startsWith);
});

const isReadOnlyProjUnderAdminUrl = computed(() => {
  return isReadOnlyProj.value && isAdminPage.value && !isAdminProjectsPage.value && !isMetricsPage.value;
});

watch(() => props.disabled, (newValue) => {
  disabledInternal.value = newValue;
});

function addClicked() {
  emit('add-action');
}

</script>

<template>
  <div class="flex flex-wrap pb-2" data-cy="subPageHeader" :class="`mb-${marginBottom}`">
    <div class="flex-1 text-left">
      <div class="text-2xl uppercase font-normal">{{ title }}</div>
    </div>
    <div class="flex pt-0 text-right" data-cy="subPageHeaderControls">
      <div v-if="!isLoading">
        <slot v-if="!isReadOnlyProjUnderAdminUrl">
          <SkillsButton ref="actionButton" v-if="action" type="button" size="small" outlined
                        id="actionButton"
                        :label="action"
                        :track-for-focus="true"
                        icon="fas fa-plus-circle"
                        :disabled="disabledInternal"
                        v-on:click="addClicked" :aria-label="ariaLabel ? ariaLabel : action"
                        :data-cy="`btn_${title}`"/>
          <i v-if="disabledInternal" class="fas fa-exclamation-circle text-warning ml-1"
             style="pointer-events: all; font-size: 1.5rem;"
             :aria-label="disabledMsg"
             v-tooltip.hover="disabledMsg"/>
        </slot>
      </div>
    </div>
    <slot name="underTitle"/>
  </div>
</template>

<style scoped></style>
