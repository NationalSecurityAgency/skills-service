<script setup>
import {ref, nextTick, computed, watch} from 'vue';
import { projConfig } from '@/components/projects/ProjConfig.js';
import {useRoute, useRouter} from "vue-router";

const emit = defineEmits(['add-action'])
const props = defineProps(['title', 'action', 'disabled', 'disabledMsg', 'ariaLabel', 'isLoading', 'marginBottom']);
const router = useRouter()
const route = useRoute()

const config = projConfig();
const actionButton = ref(null)
const disabledInternal = ref(props.disabled);

const isAdminProjectsPage = computed(() => {
  return route.path?.toLowerCase() === '/administrator/' || route.path?.toLowerCase() === '/administrator';
});

const isAdminPage = computed(() => {
  return route.path?.toLowerCase()?.startsWith('/administrator');
});

const isMetricsPage = computed(() => {
  const projId = route.params?.projectId;
  if (!projId) {
    return false;
  }
  const startsWith = `/administrator/projects/${projId}/metrics`;
  return route.path?.toLowerCase()?.startsWith(startsWith);
});

const isReadOnlyProjUnderAdminUrl = computed(() => {
  return config.isReadOnlyProj && isAdminPage && !isAdminProjectsPage && !isMetricsPage;
});

watch(() => props.disabled, (newValue) => {
  disabledInternal.value = newValue;
});

function addClicked() {
  emit('add-action');
}

function focusOnActionBtn() {
  nextTick(() => {
    if (actionButton) {
      actionButton.focus();
    }
  });
}

</script>

<template>
  <div class="grid border-bottom py-2 text-center" data-cy="subPageHeader" :class="`mb-${marginBottom}`">
    <div class="col-6 text-left">
      <div class="text-2xl uppercase font-normal">{{ title }}</div>
    </div>
    <div class="col-6 pt-0 text-right" data-cy="subPageHeaderControls">
      <div v-if="!isLoading">
        <slot v-if="!isReadOnlyProjUnderAdminUrl">
          <Button ref="actionButton" v-if="action" type="button" size="sm" variant="outline-primary"
                    :class="{'btn':true, 'btn-outline-primary':true, 'disabled':disabledInternal}"
                    v-on:click="addClicked" :aria-label="ariaLabel ? ariaLabel : action"
                    :data-cy="`btn_${title}`">
            <span class="">{{ action }} </span> <i class="fas fa-plus-circle"/>
          </Button>
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
