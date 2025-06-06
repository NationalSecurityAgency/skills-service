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
import {ref, computed, watch} from 'vue';
import { useProjConfig } from '@/stores/UseProjConfig.js'
import { useRoute } from "vue-router";

const emit = defineEmits(['add-action'])
const props = defineProps({
  'title':  String,
  'action': String,
  'disabled': Boolean,
  'disabledMsg' : String,
  'ariaLabel': String,
  'isLoading': Boolean,
  'marginBottom': String,
  titleLevel: {
    type: Number,
    default: 2
  },
});
const route = useRoute()

const config = useProjConfig();
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
      <h1 v-if="titleLevel === 1" class="text-2xl uppercase font-normal my-0">{{ title }}</h1>
      <h2 v-if="titleLevel === 2" class="text-2xl uppercase font-normal my-0">{{ title }}</h2>
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
          <div v-if="disabledInternal" class="mt-1" data-cy="subPageHeaderDisabledMsg">
            <InlineMessage severity="warn">{{ disabledMsg }}</InlineMessage>
          </div>
        </slot>
      </div>
    </div>
    <slot name="underTitle"/>
  </div>
</template>

<style scoped></style>
