<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue';
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
let badgeInternal = ref({ ...props.badge });
let cardOptions = ref({ controls: {} });
let showEditBadge = ref(false);
let showDeleteDialog = ref(false);
let deleteDisabledText = ref('');
let isDeleteDisabled = ref(false);
const navCardWithStatsAndControls = ref();
const cardNavControls = ref(null);

const isReadOnlyProj = computed(() => projConfig.isReadOnlyProj);

watch(() => props.badge, () => {
  badgeInternal.value = props.badge;
  buildCardOptions();
});

onMounted(() => {
  buildCardOptions();
});

const live = computed(() => {
  return badgeInternal.value.enabled !== 'false';
});

const buildCardOptions = () => {
  const stats = [{
    label: '# Skills',
    count: badgeInternal.value.numSkills,
    icon: 'fas fa-graduation-cap skills-color-skills',
  }];
  if (!props.global) {
    stats.push({
      label: 'Points',
      count: badgeInternal.value.totalPoints,
      icon: 'far fa-arrow-alt-circle-up skills-color-points',
    });
  } else {
    stats.push({
      label: 'Projects',
      count: badgeInternal.value.uniqueProjectCount,
      icon: 'fas fa-trophy skills-color-levels',
    });
  }
  cardOptions.value = {
    navTo: buildManageLink(),
    icon: badgeInternal.value.iconClass,
    title: badgeInternal.value.name,
    subTitle: `ID: ${badgeInternal.value.badgeId}`,
    warn: badgeInternal.value.enabled === 'false',
    warnMsg: badgeInternal.value.enabled === 'false' ? 'This badge cannot be achieved until it is live' : '',
    stats,
    controls: {
      navTo: buildManageLink(),
      type: props.global ? 'Global Badge' : 'Badge',
      name: badgeInternal.value.name,
      id: badgeInternal.value.badgeId,
      deleteDisabledText: deleteDisabledText,
      isDeleteDisabled: isDeleteDisabled,
      isFirst: badgeInternal.value.isFirst,
      isLast: badgeInternal.value.isLast,
    },
    displayOrder: props.badge.displayOrder,
  };
};

const buildManageLink = () => {
  const link = {
    name: props.global ? 'GlobalBadgeSkills' : 'BadgeSkills',
    params: {
      projectId: badgeInternal.value.projectId,
      badgeId: badgeInternal.value.badgeId,
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
  emit('badge-deleted', badgeInternal.value);
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
  emit('publish-badge', badgeInternal.value);
};
const handleHidden = (e) => {
  if (!e || !e.updated) {
    handleFocus();
  }
};
const handleFocus = () => {
  nextTick(() => {
    // cardNavControls.value.focusOnEdit();
  });
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
                                      :data-cy="`badgeCard-${badgeInternal.badgeId}`">
      <template #underTitle>
        <card-navigate-and-edit-controls ref="cardNavControls" class="mt-2"
                                         :to="buildManageLink()"
                                         :options="cardOptions.controls"
                                         :button-id-suffix="badgeInternal.badgeId"
                                         @edit="showEditBadge=true"
                                         @delete="deleteBadge"/>
      </template>
      <template #footer>
        <i v-if="badgeInternal.endDate" class="fas fa-gem absolute" style="font-size: 1rem; top: 2.6rem; left: 1.6rem; color: purple" aria-hidden="true"/>
        <div class="mt-1 row align-items-centerflex justify-content-end" style="height: 2rem;">
          <div class="col text-right small">
            <div v-if="!live" data-cy="badgeStatus" style="">
              <span class="text-secondary" style="height: 3rem;">Status: </span>
              <span class="text-uppercase" :class="{ 'border-right pr-2 mr-2' : !isReadOnlyProj }">Disabled <span class="far fa-stop-circle text-warning" aria-hidden="true"/></span>
              <SkillsButton size="small" label="Go Live" v-if="!isReadOnlyProj" data-cy="goLive" @click.stop="handlePublish"></SkillsButton>
<!--              <a href="#0" v-if="!isReadOnlyProj" @click.stop="handlePublish" class="btn btn-outline-primary btn-sm" data-cy="goLive">Go Live</a>-->
            </div>
            <div v-else data-cy="badgeStatus"  style="">
              <span class="text-secondary align-middle" style="height: 4rem;">Status: </span> <span class="text-uppercase align-middle" style="height: 4rem;">Live <span class="far fa-check-circle text-success" aria-hidden="true"/></span>
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
      :item-name="badgeInternal.name"
      item-type="badge"
      @do-remove="doDeleteBadge"
      @hidden="handleDeleteCancelled"
      :value="showDeleteDialog">
      Badge with id {{badgeInternal.badgeId}} will be removed. Deletion <b>cannot</b> be undone.
    </removal-validation>
  </div>
</template>

<style scoped></style>
