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
import { ref, nextTick, computed, onMounted } from 'vue';
import Sortable from 'sortablejs';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import GlobalBadgeService from "@/components/badges/global/GlobalBadgeService.js";
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import EditBadge from "@/components/badges/EditBadge.vue";
import LoadingContainer from "@/components/utils/LoadingContainer.vue";
import NoContent2 from "@/components/utils/NoContent2.vue";
import SkillsBadge from '@/components/badges/Badge.vue'
import { useConfirm } from 'primevue/useconfirm';
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";

const confirm = useConfirm();
const announcer = useSkillsAnnouncer();
const emit = defineEmits(['badge-deleted', 'badge-changed', 'global-badges-changed']);

const isLoading = ref(true);
const badges = ref([]);
const displayNewBadgeModal = ref(false);
const sortOrder = ref({
  loading: false,
  loadingBadgeId: '-1',
});
const badgeRef = ref([]);
const subPageHeader = ref();

const emptyNewBadge = computed(() => {
  return {
    name: '',
    badgeId: '',
    description: '',
    iconClass: 'fas fa-award',
    requiredSkills: [],
  };
});

onMounted(() => {
  loadBadges();
});

const loadBadges = (afterLoad) => {
  return GlobalBadgeService.getBadges()
      .then((badgesResponse) => {
        const localBadges = badgesResponse;
        if (localBadges && localBadges.length) {
          localBadges[0].isFirst = true;
          localBadges[localBadges.length - 1].isLast = true;
          badges.value = localBadges;
        } else {
          badges.value = [];
        }
        if (afterLoad) {
          setTimeout(() => {
            afterLoad();
          }, 0);
        }
      })
      .finally(() => {
        isLoading.value = false;
        enableDropAndDrop();
      });
};

const deleteBadge = (badge) => {
  isLoading.value = true;
  GlobalBadgeService.deleteBadge(badge.badgeId)
      .then(() => {
        emit('badge-deleted', badge);
        badges.value = badges.value.filter((item) => item.badgeId !== badge.badgeId);
        emit('badges-changed', badge.badgeId);
      })
      .finally(() => {
        isLoading.value = false;
      });
};

const saveBadge = (updatedBadge) => {
  isLoading.value = true;

  const { isEdit } = updatedBadge;

  loadBadges().then(() => {
    nextTick(() => announcer.polite(`a global badge has been ${isEdit ? 'saved' : 'created'}`));
  });
  emit('global-badges-changed', updatedBadge.badgeId);
};

const newBadge = () => {
  displayNewBadgeModal.value = true;
};

const enableDropAndDrop = () => {
  if (badges.value && badges.value.length > 0) {
    nextTick(() => {
      const cards = document.getElementById('badgeCards');
      Sortable.create(cards, {
        handle: '.sort-control',
        animation: 150,
        ghostClass: 'skills-sort-order-ghost-class',
        onUpdate(event) {
          sortOrderUpdate(event);
        },
      });
    });
  }
};

const sortOrderUpdate = (updateEvent) => {
  const { id } = updateEvent.item;
  sortOrder.value.loadingBadgeId = id;
  sortOrder.value.loading = true;
  GlobalBadgeService.updateBadgeDisplaySortOrder(id, updateEvent.newIndex)
      .finally(() => {
        sortOrder.value.loading = false;
        loadBadges().then(() => {
          // isLoadingData.value = false;
          const foundRef = badgeRef.value[id];
          nextTick(() => {
            foundRef.focusSortControl();
          });
        });
      });
};

const publishBadge = (badge) => {
  if (canPublish(badge)) {
    const msg = `While this Badge is disabled, user's cannot see the Badge or achieve it. Once the Badge is live, it will be visible to users.
        Please note that once the badge is live, it cannot be disabled.`;
    confirm.require({
      message: msg,
      header: 'Please Confirm!',
      acceptLabel: 'Yes, Go Live!',
      rejectLabel: 'Cancel',
      accept: () => {
        badge.enabled = 'true';
        const toSave = { ...badge };
        if (!toSave.originalBadgeId) {
          toSave.originalBadgeId = toSave.badgeId;
        }
        toSave.startDate = toDate(toSave.startDate);
        toSave.endDate = toDate(toSave.endDate);

        const requiredIds = badge.requiredSkills.map((item) => item.skillId);
        const badgeReq = { requiredSkillsIds: requiredIds, ...badge };
        GlobalBadgeService.saveBadge(badgeReq).then(() => {
          saveBadge(toSave);
        });
      }
    });
  } else {
    confirm.require({
      message: getNoPublishMsg(),
      header: 'Empty Badge',
      rejectClass: 'hidden',
      acceptLabel: 'OK',
    })
  }
}

const getNoPublishMsg = () => {
    return 'This Global Badge has no assigned Skills or Project Levels. A Global Badge cannot be published without at least one Skill or Project Level.';
};

const toDate = (value) => {
  let dateVal = value;
  if (value && !(value instanceof Date)) {
    dateVal = new Date(Date.parse(value.replace(/-/g, '/')));
  }
  return dateVal;
};

const canPublish = (badge) => {
  return badge.numSkills > 0 || badge.requiredProjectLevels.length > 0;
};

const handleFocus = () => {
  nextTick(() => {

  })
}
</script>

<template>
  <div>
    <sub-page-header ref="subPageHeader" title="Global Badges" action="Badge" @add-action="newBadge" aria-label="new global badge"/>
<!--      <transition name="projectContainer" enter-active-class="animated fadeIn">-->
      <div>
        <div v-if="(!badges || badges.length === 0) && isLoading">
          <skills-spinner :is-loading="isLoading" label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
        </div>
        <div v-if="badges && badges.length" id="badgeCards" class="flex flex-wrap align-items-center justify-content-center">
          <div v-for="(badge) of badges" :id="badge.badgeId" :key="badge.badgeId" class="lg:col-4 mb-3"  style="min-width: 23rem;">
            <BlockUI :blocked="sortOrder.loading">
              <div class="absolute z-5 top-50 w-full text-center" v-if="sortOrder.loading" :data-cy="`${badge.badgeId}_overlayShown`">
                <div v-if="badge.badgeId===sortOrder.loadingBadgeId" data-cy="updatingSortMsg">
                  <div class="text-info text-uppercase mb-1">Updating sort order!</div>
                  <skills-spinner :is-loading="sortOrder.loading" label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
                </div>
              </div>

              <SkillsBadge :badge="badge" :global="true"
                           @badge-updated="saveBadge"
                           @badge-deleted="deleteBadge"
                           @publish-badge="publishBadge"
                           @sort-changed-requested="sortOrderUpdate"
                           :ref="(el) => (badgeRef[badge.badgeId] = el)"
                           :disable-sort-control="badges.length === 1"/>
            </BlockUI>
          </div>
        </div>

        <no-content2 v-else title="No Badges Yet" class="mt-4"
                       message="Global Badges are a special kind of badge that is made up of a collection of skills and/or levels that span across project boundaries."/>
      </div>
<!--      </transition>-->

    <edit-badge v-if="displayNewBadgeModal" v-model="displayNewBadgeModal" :badge="emptyNewBadge"
                :global="true" @badge-updated="saveBadge" @hidden="handleFocus"></edit-badge>
  </div>
</template>

<style scoped>

</style>