<script setup>
import { ref, nextTick, computed, onMounted } from 'vue';
import Sortable from 'sortablejs';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import GlobalBadgeService from "@/components/badges/global/GlobalBadgeService.js";
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import EditBadge from "@/components/badges/EditBadge.vue";
import LoadingContainer from "@/components/utils/LoadingContainer.vue";
import NoContent2 from "@/components/utils/NoContent2.vue";

const announcer = useSkillsAnnouncer();
const emit = defineEmits(['badge-deleted', 'badge-changed', 'global-badges-changed']);

const isLoading = ref(true);
const badges = ref([]);
const displayNewBadgeModal = ref(false);
const sortOrder = ref({
  loading: false,
  loadingBadgeId: '-1',
});

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

const saveBadge = (badge) => {
  isLoading.value = true;
  const requiredIds = badge.requiredSkills.map((item) => item.skillId);
  const badgeReq = { requiredSkillsIds: requiredIds, ...badge };
  const { isEdit } = badge;
  GlobalBadgeService.saveBadge(badgeReq)
      .then(() => {
        let afterLoad = null;
        if (isEdit) {
          afterLoad = () => {
            const refKey = `badge_${badgeReq.badgeId}`;
            // const ref = $refs[refKey];
            // if (ref) {
            //   ref[0].handleFocus();
            // }
          };
        }
        loadBadges(afterLoad).then(() => {
          nextTick(() => announcer.polite(`a global badge has been ${isEdit ? 'saved' : 'created'}`));
        });
        emit('global-badges-changed', badge.badgeId);
      });
};

const newBadge = () => {
  displayNewBadgeModal.value = true;
};

const handleHidden = (event) => {
  if (!event || !event.update) {
    handleFocus();
  }
};

const handleFocus = () => {
  nextTick(() => {
    // $refs.subPageHeader.$refs.actionButton.focus();
  });
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
      });
};
</script>

<template>
  <div>
    <sub-page-header ref="subPageHeader" title="Global Badges" action="Badge" @add-action="newBadge" aria-label="new global badge"/>
    <loading-container v-bind:is-loading="isLoading">
      <transition name="projectContainer" enter-active-class="animated fadeIn">
        <div>
          <div v-if="badges && badges.length" id="badgeCards"
               class="row justify-content-center">
            <div v-for="(badge) of badges" :id="badge.badgeId"
                 :key="badge.badgeId" class="col-lg-4 mb-3"  style="min-width: 23rem;">
              <BlockUI :blocked="sortOrder.loading" rounded="sm" opacity="0.4">
<!--                <template #overlay>-->
<!--                  <div class="text-center" :data-cy="`${badge.badgeId}_overlayShown`">-->
<!--                    <div v-if="badge.badgeId===sortOrder.loadingBadgeId" data-cy="updatingSortMsg">-->
<!--                      <div class="text-info text-uppercase mb-1">Updating sort order!</div>-->
<!--                      <b-spinner label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>-->
<!--                    </div>-->
<!--                  </div>-->
<!--                </template>-->

                <badge :badge="badge" :global="true"
                       @badge-updated="saveBadge"
                       @badge-deleted="deleteBadge"
                       :ref="`badge_${badge.badgeId}`"
                       :disable-sort-control="badges.length === 1"/>
              </BlockUI>
            </div>
          </div>

          <no-content2 v-else title="No Badges Yet" class="mt-4"
                       message="Global Badges are a special kind of badge that is made up of a collection of skills and/or levels that span across project boundaries."/>
        </div>
      </transition>
    </loading-container>

    <edit-badge v-if="displayNewBadgeModal" v-model="displayNewBadgeModal" :badge="emptyNewBadge"
                :global="true" @badge-updated="saveBadge"
                @hidden="handleHidden"></edit-badge>
  </div>
</template>

<style scoped>

</style>