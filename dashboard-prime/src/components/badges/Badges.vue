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
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { SkillsReporter } from '@skilltree/skills-client-js'
import Sortable from 'sortablejs'
import BlockUI from 'primevue/blockui'
import LoadingContainer from '@/components/utils/LoadingContainer.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import BadgesService from '@/components/badges/BadgesService'
import Badge from '@/components/badges/Badge.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import EditBadge from '@/components/badges/EditBadge.vue'
import { useConfirm } from 'primevue/useconfirm'
import { useElementHelper } from '@/components/utils/inputForm/UseElementHelper.js';

const announcer = useSkillsAnnouncer()
const projConfig = useProjConfig();
// const props = defineProps(['subject']);
const emit = defineEmits(['badge-deleted', 'badges-changed']);
const appConfig = useAppConfig()
const route = useRoute();
const confirm = useConfirm();
const elementHelper = useElementHelper()

let global = ref(false);
let isLoadingData = ref(true);
let badges = ref([]);
let displayNewBadgeModal = ref(false);
let projectId = ref(null);
let sortOrder = ref({
  loading: false,
  loadingBadgeId: '-1',
});
const badgeRef = ref([]);

const isReadOnlyProj = computed(() => projConfig.isReadOnlyProj);

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
  return badges.value && badges.value.length >= appConfig.maxBadgesPerProject;
});

const addBadgesDisabledMsg = computed(() => {
  return `The maximum number of Badges allowed is ${appConfig.maxBadgesPerProject}`
})

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
                const foundRef = badgeRef.value[updateInfo.id];
                nextTick(() => {
                  foundRef.focusSortControl();
                });
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
  BadgesService.saveBadge(badge).then(() => {
    badgeUpdated(badge);
  });
}

const badgeUpdated = (badge) => {
  isLoadingData.value = true;
  const requiredIds = badge.requiredSkills.map((item) => item.skillId);
  const badgeReq = { requiredSkillsIds: requiredIds, ...badge };
  const { isEdit } = badge;

  let afterLoad = null;
  //   if (isEdit) {
  afterLoad = () => {
    const refKey = `badge_${badgeReq.badgeId}`;
        // const ref = $refs[refKey];
        // if (ref) {
        //   ref[0].handleFocus();
        // }
  };
  //   }
  loadBadges(afterLoad).then(() => {
    const msg = isEdit ? 'edited' : 'created';
    nextTick(() => announcer.polite(`Badge ${badge.name} has been ${msg}`));
  });
  //   // loadProjectDetailsState({ projectId: projectId.value });
  emit('badges-changed', badge.badgeId);
  // });
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
  displayNewBadgeModal.value = false;
};

const enableDragAndDrop = () => {
  if (badges.value && badges.value.length > 0) {
    nextTick(() => {
      elementHelper.getElementById('badgeCards').then((cards) => {
        Sortable.create(cards, {
          handle: '.sort-control',
          animation: 150,
          ghostClass: 'skills-sort-order-ghost-class',
          onUpdate(event) {
            sortOrderUpdate(event);
          },
        });
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

const canPublish = (badge) => {
  if (global.value) {
    return badge.numSkills > 0 || badge.requiredProjectLevels.length > 0;
  }

  return badge.numSkills > 0;
};
const getNoPublishMsg = () => {
  let msg = 'This Badge has no assigned Skills. A Badge cannot be published without at least one assigned Skill.';
  if (global.value) {
    msg = 'This Global Badge has no assigned Skills or Project Levels. A Global Badge cannot be published without at least one Skill or Project Level.';
  }

  return msg;
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
        saveBadge(toSave);
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

const toDate = (value) => {
  let dateVal = value;
  if (value && !(value instanceof Date)) {
    dateVal = new Date(Date.parse(value.replace(/-/g, '/')));
  }
  return dateVal;
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
            <div v-for="(badge) of badges" :id="badge.badgeId" :key="badge.badgeId" class="lg:col-4 mb-3"  style="min-width: 23rem;">
              <BlockUI :blocked="sortOrder.loading">
                <div class="absolute z-5 top-50 w-full text-center" v-if="sortOrder.loading" :data-cy="`${badge.badgeId}_overlayShown`">
                  <div v-if="badge.badgeId===sortOrder.loadingBadgeId" data-cy="updatingSortMsg">
                    <div class="text-info text-uppercase mb-1">Updating sort order!</div>
                    <skills-spinner :is-loading="sortOrder.loading" label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
                  </div>
                </div>

                <badge :badge="badge"
                       :ref="(el) => (badgeRef[badge.badgeId] = el)"
                       @badge-updated="badgeUpdated"
                       @badge-deleted="deleteBadge"
                       @sort-changed-requested="updateSortAndReloadSubjects"
                       @publish-badge="publishBadge"
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

    <edit-badge v-if="displayNewBadgeModal" v-model="displayNewBadgeModal"
                :badge="emptyNewBadge"
                @badge-updated="badgeUpdated"
                @hidden="handleHidden"></edit-badge>
  </div>
</template>

<style scoped></style>
