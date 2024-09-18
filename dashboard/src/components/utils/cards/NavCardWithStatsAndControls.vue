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
import { computed, ref } from 'vue'
import Badge from 'primevue/badge'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import CardWithVericalSections from '@/components/utils/cards/CardWithVericalSections.vue'

const projConfig = useProjConfig();
const props = defineProps(['options', 'disableSortControl']);
const emit = defineEmits(['sort-changed-requested']);

const isReadOnlyProj = computed(() => projConfig.isReadOnlyProj);
let overSortControl = ref(false);
const sortControl = ref();

const moveDown = () => {
    emit('sort-changed-requested', {
      direction: 'down',
    });
};

const moveUp = () => {
    emit('sort-changed-requested', {
      direction: 'up',
    });
};

const focusSortControl = () => {
  sortControl.value.focus();
};

defineExpose({
  focusSortControl
})
</script>

<template>
  <CardWithVericalSections class="h-full">
    <template #content>
      <div class="flex mb-2">
        <div class="flex flex-1 pt-4 pl-4">
          <router-link v-if="options.icon"
                       :to="options.navTo" aria-label="Navigate to Skills" data-cy="iconLink" aria-hidden="true"
                       tabindex="-1" class="">
            <div class="d-inline-block mr-2 border-1 text-center border-round w-4rem subject-icon-container"
                 aria-hidden="true">
              <i :class="[`${options.icon} subject-icon`]" aria-hidden="true" />
            </div>
          </router-link>
          <div class="media-body">
            <div
              class="text-xl font-semibold no-underline overflow-hidden text-overflow-ellipsis white-space-nowrap"
              style="max-width:17rem">
              <router-link v-if="options.icon" :to="options.navTo" data-cy="titleLink" class="no-underline"
                           :aria-label="`${isReadOnlyProj ? 'View' : 'Manage'} ${options.controls.type} ${options.controls.name}`">
                {{ options.title }}
              </router-link>
            </div>
            <div class="text-secondary text-xs overflow-hidden text-overflow-ellipsis white-space-nowrap"
                 style="max-width:15rem"
                 data-cy="subTitle">{{ options.subTitle }}
            </div>
          </div>
        </div>
        <div>
          <div v-if="!disableSortControl && !isReadOnlyProj"
               ref="sortControl"
               @mouseover="overSortControl = true"
               @mouseleave="overSortControl = false"
               @keyup.down="moveDown"
               @keyup.up="moveUp"
               @click.prevent.self
               class="px-2 py-1 text-xl border-left-1 border-bottom-1 border-round-bottom-left-md surface-border text-color-secondary hover:shadow-3 hover:text-primary"
               tabindex="0"
               :aria-label="`Sort Control. Current position for ${options.title} is ${options.displayOrder}. Press up or down to change the order.`"
               role="button"
               data-cy="sortControlHandle"><i class="fas fa-arrows-alt"></i>
          </div>
        </div>
      </div>

      <div class="mt-4 px-4">
        <slot name="underTitle"></slot>
      </div>

      <div class="flex gap-2 flex-column sm:flex-row text-center justify-content-center my-3 px-4">
        <div v-for="(stat) in options.stats" :key="stat.label" class="flex-1" style="min-width: 10rem;">
          <div :data-cy="`pagePreviewCardStat_${stat.label}`" class="h-full border-round border-1 border-300 surface-100 p-3">
            <i :class="stat.icon" class="text-xl"></i>
            <div class="uppercase mt-1">{{ stat.label }}</div>
            <div class="text-2xl mt-2" data-cy="statNum">{{ stat.count }}</div>
            <div v-if="stat.secondaryStats">
              <div v-for="secCount in stat.secondaryStats" :key="secCount.label">
                <div v-if="secCount.count > 0" style="font-size: 0.9rem">
                  <Badge :variant="`${secCount.badgeVariant}`"
                           :data-cy="`pagePreviewCardStat_${stat.label}_${secCount.label}`">
                    <span>{{ secCount.count }}</span>
                  </Badge>
                  <span class="text-left text-uppercase ml-1" style="font-size: 0.8rem">{{ secCount.label }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="options.warnMsg" class="mt-1 px-4 pb-3">
        <InlineMessage icon="fas fa-exclamation-circle" severity="warn">{{ options.warnMsg}}</InlineMessage>
      </div>
    </template>
    <template #footer>
      <div class="px-4 pb-4">
        <slot name="footer"></slot>
      </div>
    </template>


  </CardWithVericalSections>
</template>

<style scoped>
.border-round-bottom-left-md {
  border-bottom-left-radius: 0.375rem !important;
}

.subject-icon {
  height: 100%;
  width: 100%;
  background-size: cover;
  background-position: center;
  font-size: 42px !important;
  line-height: 58px;
}

.subject-icon-container {
  max-width:100px;
  max-height:100px;
  height:60px;
  width: 60px;
}
</style>
