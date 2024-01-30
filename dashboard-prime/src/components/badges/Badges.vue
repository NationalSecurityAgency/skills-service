<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue';
import { useStore } from 'vuex'
import { useRoute } from 'vue-router'
import { SkillsReporter } from '@skilltree/skills-client-js';
import Sortable from 'sortablejs';
import BlockUI from 'primevue/blockui';
import LoadingContainer from '@/components/utils/LoadingContainer.vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import NoContent2 from "@/components/utils/NoContent2.vue";
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import {projConfig} from "@/components/projects/ProjConfig.js";
import BadgesService from '@/components/badges/BadgesService';
import Badge from '@/components/badges/Badge.vue';

const announcer = useSkillsAnnouncer()
// const config = projConfig();
// const props = defineProps(['subject']);
const emit = defineEmits(['badge-deleted', 'badges-changed']);
const store = useStore();
const route = useRoute();

let isLoadingData = ref(true);
let badges = ref([]);
let displayNewBadgeModal = ref(false);
let projectId = ref(null);
let sortOrder = ref({
  loading: false,
  loadingBadgeId: '-1',
});

let isReadOnlyProj = false;

onMounted(() => {
  projectId.value = route.params.projectId;
  loadBadges();
});

const isLoading = computed(() => {
  return isLoadingData.value; // || isLoadingProjConfig;
});

const emptyNewBadge = computed(() => {
  return {
    projectId: projectId.value,
    name: '',
    badgeId: '',
    description: '',
    iconClass: 'fas fa-award',
    requiredSkills: [],
  };
});

const addBadgeDisabled = computed(() => {
  return badges.value && store.getters.config && badges.value.length >= store.getters.config.maxBadgesPerProject;
});

const addBadgesDisabledMsg = computed(() => {
  if (store.getters.config) {
    return `The maximum number of Badges allowed is ${store.getters.config.maxBadgesPerProject}`;
  }
  return '';
});

const loadBadges = (afterLoad) => {
  return BadgesService.getBadges(projectId.value)
      .then((badgesResponse) => {
        isLoadingData.value = false;
        badges.value = badgesResponse;
        if (badges.value && badges.value.length) {
          badges.value[0].isFirst = true;
          badges.value[badges.value.length - 1].isLast = true;
        }
        if (afterLoad) {
          nextTick(() => {
            afterLoad();
          });
        }
      })
      .finally(() => {
        isLoadingData.value = false;
        enableDragAndDrop();
      });
};

const updateSortAndReloadSubjects = (updateInfo) => {
  const sortedBadges = badges.value.sort((a, b) => {
    if (a.displayOrder > b.displayOrder) {
      return 1;
    }
    if (b.displayOrder > a.displayOrder) {
      return -1;
    }
    return 0;
  });
  const currentIndex = sortedBadges.findIndex((item) => item.badgeId === updateInfo.id);
  const newIndex = updateInfo.direction === 'up' ? currentIndex - 1 : currentIndex + 1;
  if (newIndex >= 0 && (newIndex) < badges.value.length) {
    isLoadingData.value = true;
    BadgesService.updateBadgeDisplaySortOrder(projectId.value, updateInfo.id, newIndex)
        .finally(() => {
          loadBadges()
              .then(() => {
                isLoadingData.value = false;
                // const foundRef = $refs[`badge_${updateInfo.id}`];
                // nextTick(() => {
                //   foundRef[0].focusSortControl();
                // });
              });
        });
  }
};

const deleteBadge = (badge) => {
  isLoadingData.value = true;
  BadgesService.deleteBadge(badge.projectId, badge.badgeId)
      .then(() => {
        emit('badge-deleted', badge);
        badges.value = badges.value.filter((item) => item.badgeId !== badge.badgeId);
        // loadProjectDetailsState({ projectId: projectId.value });
        emit('badges-changed', badge.badgeId);
      })
      .then(() => {
        setTimeout(() => announcer.polite(`Badge ${badge.name} has been deleted`), 0);
      })
      .finally(() => {
        isLoadingData.value = false;
      });
};

const saveBadge = (badge) => {
  isLoadingData.value = true;
  const requiredIds = badge.requiredSkills.map((item) => item.skillId);
  const badgeReq = { requiredSkillsIds: requiredIds, ...badge };
  const { isEdit } = badge;
  BadgesService.saveBadge(badgeReq).then(() => {
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
      const msg = isEdit ? 'edited' : 'created';
      nextTick(() => announcer.polite(`Badge ${badge.name} has been ${msg}`));
    });
    // loadProjectDetailsState({ projectId: projectId.value });
    emit('badges-changed', badge.badgeId);
  });
  if (badge.startDate) {
    SkillsReporter.reportSkill('CreateGem');
  } else {
    SkillsReporter.reportSkill('CreateBadge');
  }
};

const newBadge = () => {
  displayNewBadgeModal.value = true;
};

const handleHidden = (e) => {
  if (!e || !e.update) {
    handleFocus();
  }
};

const handleFocus = () => {
  nextTick(() => {
    // $refs.subPageHeader.$refs.actionButton.focus();
  });
};

const enableDragAndDrop = () => {
  if (badges.value && badges.value.length > 0 && projConfig && !isReadOnlyProj) {
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
  BadgesService.updateBadgeDisplaySortOrder(projectId.value, id, updateEvent.newIndex)
      .finally(() => {
        sortOrder.value.loading = false;
        sortOrder.value.loadingBadgeId = '-1';
        SkillsReporter.reportSkill('ChangeBadgeDisplayOrder');
      });
};
</script>

<template>
  <div>
    <sub-page-header ref="subPageHeader" title="Badges" action="Badge" @add-action="newBadge"
                     :disabled="addBadgeDisabled"
                     :disabled-msg="addBadgesDisabledMsg" aria-label="new badge"/>
    <loading-container v-bind:is-loading="isLoading">
<!--      <transition name="projectContainer" enter-active-class="animated fadeIn">-->
        <div>
          <div v-if="badges && badges.length" id="badgeCards" class="flex flex-wrap align-items-center justify-content-center">
            <div v-for="(badge) of badges" :id="badge.badgeId"
                 :key="badge.badgeId" class="lg:col-4 mb-3"  style="min-width: 23rem;">
              <BlockUI :blocked="sortOrder.loading">
                <div class="absolute z-5 top-50 w-full text-center" :data-cy="`${badge.badgeId}_overlayShown`">
                  <div v-if="badge.badgeId===sortOrder.loadingBadgeId" data-cy="updatingSortMsg">
                    <div class="text-info text-uppercase mb-1">Updating sort order!</div>
                    <skills-spinner label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
                  </div>
                </div>

                <badge :badge="badge"
                       :ref="'badge_'+badge.badgeId"
                       @badge-updated="saveBadge"
                       @badge-deleted="deleteBadge"
                       @sort-changed-requested="updateSortAndReloadSubjects"
                       :disable-sort-control="badges.length === 1"/>
              </BlockUI>
            </div>
          </div>

          <no-content2 v-else title="No Badges Yet"
                       message="Badges add another facet to the overall gamification profile and allows you to further reward your users by providing these prestigious symbols. Badges are a collection of skills and when all of the skills are accomplished that badge is earned."
                       class="mt-4"/>
        </div>
<!--      </transition>-->
    </loading-container>

<!--    <edit-badge v-if="displayNewBadgeModal" v-model="displayNewBadgeModal"-->
<!--                :badge="emptyNewBadge"-->
<!--                @badge-updated="saveBadge"-->
<!--                @hidden="handleHidden"></edit-badge>-->
  </div>
</template>

<style scoped></style>