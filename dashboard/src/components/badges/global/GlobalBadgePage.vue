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
import { useRoute, useRouter } from 'vue-router';
import Navigation from "@/components/utils/Navigation.vue";
import EditBadge from "@/components/badges/EditBadge.vue";
import PageHeader from "@/components/utils/pages/PageHeader.vue";
import GlobalBadgeService from "@/components/badges/global/GlobalBadgeService.js";
import {useBadgeState} from "@/stores/UseBadgeState.js";
import {storeToRefs} from "pinia";
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";
import Avatar from 'primevue/avatar'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useCommunityLabels } from '@/components/utils/UseCommunityLabels.js'

const dialogMessages = useDialogMessages()
const route = useRoute();
const router = useRouter();
const communityLabels = useCommunityLabels()

const isLoading = ref(true);
const badgeId = ref(route.params.badgeId);
const showEdit = ref(false);
const badgeState = useBadgeState();
const appConfig = useAppConfig()
const { badge } = storeToRefs(badgeState);

onMounted(() => {
  loadBadge();
});

const headerOptions = computed(() => {
  if (!badge.value) {
    return {};
  }
  return {
    icon: 'fas fa-globe-americas skills-color-badges',
    title: `BADGE: ${badge.value.name}`,
    subTitle: `ID: ${badge.value.badgeId}`,
    stats: [{
      label: 'Status',
      icon: (badge.value.enabled === 'true' ? 'far fa-check-circle text-success' : 'far fa-stop-circle text-warning'),
      preformatted: `<div class="h5 font-weight-bold mb-0">${badge.value.enabled === 'true' ? 'Live' : 'Disabled'}</div>`,
    }, {
      label: 'Skills',
      count: badge.value.numSkills,
      icon: 'fas fa-graduation-cap skills-color-skills',
    }, {
      label: 'Levels',
      count: badge.value.requiredProjectLevels?.length,
      icon: 'fas fa-trophy skills-color-levels',
    }, {
      label: 'Projects',
      count: badge.value.uniqueProjectCount,
      icon: 'fas fa-project-diagram skills-color-projects',
    }],
  };
});

const displayEditBadge = () => {
  showEdit.value = true;
};

const loadBadge = () => {
  isLoading.value = false;
  if (route.params.badge) {
    badge.value = route.params.badge;
    isLoading.value = false;
  } else {
    badgeState.loadGlobalBadgeDetailsState(badgeId.value).finally(() => {
      badge.value = badgeState.badge;
      isLoading.value = false;
    });
  }
};

const goLive = (editedBadge) => {
  GlobalBadgeService.saveBadge(editedBadge).then((resp) => {
    badgeEdited(resp)
  });

};
const badgeEdited = (editedBadge) => {
  const origId = badge.value.badgeId;
  badgeState.loadGlobalBadgeDetailsState(editedBadge.badgeId).finally(() => {
    badge.value = badgeState.badge;
    if (origId !== editedBadge.badgeId) {
      badgeId.value = editedBadge.badgeId;
      router.replace({ name: route.name, params: { ...route.params, badgeId: editedBadge.badgeId } });
    }
  });
};

const handleHidden = (e) => {
  if (!e || !e.saved) {
    // handleFocus();
  }
};


const handlePublish = () => {
  if (canPublish()) {
    const msg = `While this Badge is disabled, user's cannot see the Badge or achieve it. Once the Badge is live, it will be visible to users.
        Please note that once the badge is live, it cannot be disabled.`;

    dialogMessages.msgConfirm({
      message: msg,
      header: 'Please Confirm!',
      acceptLabel: 'Yes, Go Live!',
      rejectLabel: 'Cancel',
      accept: () => {
        badge.value.enabled = 'true';
        const toSave = { ...badge.value };
        if (!toSave.originalBadgeId) {
          toSave.originalBadgeId = toSave.badgeId;
        }
        toSave.startDate = toDate(toSave.startDate);
        toSave.endDate = toDate(toSave.endDate);
        toSave.isEdit = true
        toSave.enableProtectedUserCommunity = communityLabels.isRestrictedUserCommunity(toSave.userCommunity)
        goLive(toSave);
      }
    });
  } else {
    dialogMessages.msgOk({
      message: getNoPublishMsg(),
      header: 'Empty Badge',
    })
  }
};

const canPublish = () => {
  return badge.value.numSkills > 0 || badge.value.requiredProjectLevels.length > 0;
};

const getNoPublishMsg = () => {
  const msg = 'This Global Badge has no assigned Skills or Project Levels. A Global Badge cannot be published without at least one Skill or Project Level.';

  return msg;
};

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
    <page-header :loading="isLoading" :options="headerOptions">
      <template #right-of-header>
        <i v-if="badge && badge.endDate" class="fas fa-gem ml-2" style="font-size: 1.6rem; color: purple;"></i>
      </template>
      <template #subSubTitle v-if="badge">
        <div>
          <div>
            <div>UC Below: </div>
            <div v-if="badgeState.badge.userCommunity" class="my-1" data-cy="userCommunity">
              <Avatar icon="fas fa-shield-alt" class="text-red-500"></Avatar>
              <span
                  class="text-secondary font-italic ml-1">{{ appConfig.userCommunityBeforeLabel }}</span> <span
                class="text-primary">{{ badgeState.badge.userCommunity }}</span> <span
                class="text-secondary font-italic">{{ appConfig.userCommunityAfterLabel }}</span>
            </div>
          </div>
          <div>
            <ButtonGroup>
              <SkillsButton @click="displayEditBadge"
                        ref="editBadgeButton"
                        class="btn btn-outline-primary"
                        size="small"
                        id="editBadgeButton"
                        data-cy="btn_edit-badge"
                        :aria-label="'edit Badge '+badge.badgeId"
                        label="Edit"
                        :track-for-focus="true"
                        icon="fas fa-edit">
              </SkillsButton>
              <SkillsButton v-if="badge.enabled !== 'true'"
                        @click.stop="handlePublish"
                        class="btn btn-outline-primary"
                        size="small"
                        aria-label="Go Live"
                        id="globalBadgeGoLiveButton"
                        :track-for-focus="true"
                        data-cy="goLive" label="Go Live">
              </SkillsButton>
            </ButtonGroup>
            </div>
        </div>
      </template>
    </page-header>

    <navigation :nav-items="[
          {name: 'Skills', iconClass: 'fa-graduation-cap skills-color-skills', page: 'GlobalBadgeSkills'},
          {name: 'Levels', iconClass: 'fa-trophy skills-color-levels', page: 'GlobalBadgeLevels'},
          { name: 'Access', iconClass: 'fas fa-shield-alt', page: 'GlobalBadgeAccessPage' }
        ]">
    </navigation>
    <edit-badge v-if="showEdit" v-model="showEdit" :id="badge.badgeId" :badge="badge" :is-edit="true"
                :global="true" @badge-updated="badgeEdited" @hidden="handleHidden"></edit-badge>
  </div>
</template>

<style scoped>

</style>