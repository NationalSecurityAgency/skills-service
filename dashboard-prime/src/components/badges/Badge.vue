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
import { ref, computed, onMounted, nextTick } from 'vue';
import NavCardWithStatsAndControls from '@/components/utils/cards/NavCardWithStatsAndControls.vue';
import CardNavigateAndEditControls from '@/components/utils/cards/CardNavigateAndEditControls.vue';
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue';
import EditBadge from "@/components/badges/EditBadge.vue";
import { useProjConfig } from '@/stores/UseProjConfig.js'

const projConfig = useProjConfig();
const props = defineProps({
  badge: Object,
  global: {
    type: Boolean,
    default: false,
  },
  disableSortControl: {
    type: Boolean,
    default: false,
  }}
);

const emit = defineEmits(['sort-changed-requested', 'badge-updated', 'badge-deleted', 'publish-badge']);

let isLoading = ref(false);
// let cardOptions = ref({ controls: {} });
let showEditBadge = ref(false);
let showDeleteDialog = ref(false);
const navCardWithStatsAndControls = ref();
const cardNavControls = ref(null);

const isReadOnlyProj = computed(() => projConfig.isReadOnlyProj);

const cardOptions = computed(() => {
  const stats = [{
    label: '# Skills',
    count: props.badge.numSkills,
    icon: 'fas fa-graduation-cap skills-color-skills',
  }];
  if (!props.global) {
    stats.push({
      label: 'Points',
      count: props.badge.totalPoints,
      icon: 'far fa-arrow-alt-circle-up skills-color-points',
    });
  } else {
    stats.push({
      label: 'Projects',
      count: props.badge.uniqueProjectCount,
      icon: 'fas fa-trophy skills-color-levels',
    });
  }
  return {
    navTo: buildManageLink(),
    icon: props.badge.iconClass,
    title: props.badge.name,
    subTitle: `ID: ${props.badge.badgeId}`,
    stats,
    controls: {
      navTo: buildManageLink(),
      type: props.global ? 'Global Badge' : 'Badge',
      name: props.badge.name,
      id: props.badge.badgeId,
      deleteDisabledText: '',
      isDeleteDisabled: false,
      isFirst: props.badge.isFirst,
      isLast: props.badge.isLast,
    },
    displayOrder: props.badge.displayOrder,
  };
})

const live = computed(() => {
  return props.badge.enabled !== 'false';
});

const buildManageLink = () => {
  const link = {
    name: props.global ? 'GlobalBadgeSkills' : 'BadgeSkills',
    params: {
      projectId: props.badge.projectId,
      badgeId: props.badge.badgeId,
    },
  };
  return link;
};
const deleteBadge = () => {
  showDeleteDialog.value = true;
};
const doDeleteBadge = () => {
  badgeDeleted();
};
const badgeEdited = (badge) => {
  emit('badge-updated', badge);
};
const badgeDeleted = () => {
  emit('badge-deleted', props.badge);
};
const sortRequested = (info) => {
  const withId = {
    ...info,
    id: props.badge.badgeId,
  };
  emit('sort-changed-requested', withId);
};

const focusSortControl = () => {
  navCardWithStatsAndControls.value.focusSortControl();
};
const handlePublish = () => {
  emit('publish-badge', props.badge);
};

const handleDeleteCancelled = () => {
  nextTick(() => {
    cardNavControls.value.focusOnDelete();
  });
};

defineExpose({
  focusSortControl
});
</script>

<template>
  <div data-cy="badgeCard">
    <nav-card-with-stats-and-controls :options="cardOptions" :isLoading="isLoading"
                                      :disable-sort-control="disableSortControl"
                                      ref="navCardWithStatsAndControls" @sort-changed-requested="sortRequested"
                                      :data-cy="`badgeCard-${badge.badgeId}`">
      <template #underTitle>
        <card-navigate-and-edit-controls ref="cardNavControls" class="mt-2"
                                         :to="buildManageLink()"
                                         :options="cardOptions.controls"
                                         :button-id-suffix="badge.badgeId"
                                         @edit="showEditBadge=true"
                                         @delete="deleteBadge"/>
      </template>
      <template #footer>
        <i v-if="badge.endDate" class="fas fa-gem absolute" style="font-size: 1rem; top: 2.6rem; left: 1.6rem; color: purple" aria-hidden="true"/>
        <div class="mt-1" style="height: 2.5rem;">
          <div v-if="!live" data-cy="badgeStatus" class="flex align-items-end">
            <div class="flex-1">
              <span class="text-color-secondary">Status: </span>
              <span class="uppercase" :class="{ 'border-right pr-2 mr-2' : !isReadOnlyProj }">Disabled <span
                class="far fa-stop-circle text-red-500" aria-hidden="true" /></span>
            </div>
            <div>
              <SkillsButton size="small" label="Go Live" v-if="!isReadOnlyProj" data-cy="goLive"
                          @click.stop="handlePublish"></SkillsButton>
            </div>
          </div>
          <div v-else data-cy="badgeStatus" class="flex align-items-end h-full">
            <div class="flex-1">
              <span class="text-color-secondary" style="height: 4rem;">Status: </span> <span
              class="uppercase" style="height: 4rem;">Live <span
              class="far fa-check-circle text-green-500" aria-hidden="true" /></span>
            </div>
          </div>
        </div>

        <edit-badge v-if="showEditBadge" v-model="showEditBadge" :id="badge.badgeId" :badge="badge" :is-edit="true"
                    :global="global" @badge-updated="badgeEdited" @hidden="handleFocus"></edit-badge>
      </template>
    </nav-card-with-stats-and-controls>
    <removal-validation
      v-if="showDeleteDialog"
      v-model="showDeleteDialog"
      :item-name="badge.name"
      item-type="badge"
      @do-remove="doDeleteBadge"
      @hidden="handleDeleteCancelled"
      :value="showDeleteDialog">
      Badge with id {{badge.badgeId}} will be removed. Deletion <b>cannot</b> be undone.
    </removal-validation>
  </div>
</template>

<style scoped></style>
