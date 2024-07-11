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
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import PageHeader from '@/components/utils/pages/PageHeader.vue';
import Navigation from '@/components/utils/Navigation.vue';
import { useProjConfig } from '@/stores/UseProjConfig.js'
import BadgesService from '@/components/badges/BadgesService';
import { useBadgeState } from '@/stores/UseBadgeState.js';
import { storeToRefs } from 'pinia';
import EditBadge from "@/components/badges/EditBadge.vue";
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";

const dialogMessages = useDialogMessages()
const route = useRoute();
const router = useRouter();
const projConfig = useProjConfig();
const badgeState = useBadgeState();
const { badge } = storeToRefs(badgeState);
const isReadOnlyProj = computed(() => projConfig.isReadOnlyProj);

const navItems = [
  {name: 'Skills', iconClass: 'fa-graduation-cap skills-color-skills', page: 'BadgeSkills'},
  {name: 'Users', iconClass: 'fa-users skills-color-users', page: 'BadgeUsers'},
];

let isLoading = ref(false);
let projectId = ref(route.params.projectId);
let badgeId = ref(route.params.badgeId);
let showEditBadge = ref(false);

onMounted(() => {
  loadBadge();
});

const headerOptions = computed(() => {
  if (!badge.value) {
    return {};
  }
  return {
    icon: 'fas fa-award skills-color-badges',
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
      label: 'Points',
      count: badge.value.totalPoints,
      icon: 'far fa-arrow-alt-circle-up skills-color-points',
    }],
  };
});

const displayEditBadge = () => {
  showEditBadge.value = true;
};

const loadBadge = () => {
  isLoading.value = false;
  if (route.params.badge) {
    badge.value = route.params.badge;
    isLoading.value = false;
  } else {
    badgeState.loadBadgeDetailsState(projectId.value, badgeId.value).finally(() => {
      badge.value = badgeState.badge;
      isLoading.value = false;
    });
  }
};

const saveBadge = (badge) => {
  isLoading.value = true;
  BadgesService.saveBadge(badge).then(() => {
    badgeEdited(badge);
  });
}

const badgeEdited = (editedBadge) => {
  const origId = badge.value.badgeId;
  badgeState.badge = editedBadge;
  if (origId !== editedBadge.badgeId) {
    router.replace({ name: route.name, params: { ...route.params, badgeId: editedBadge.badgeId } });
    badge.value = editedBadge;
    badgeId.value = editedBadge.badgeId;
  }
  isLoading.value = false;
};

const handleHidden = (e) => {
  showEditBadge.value = false;
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
        const toSave = {...badge.value};
        if (!toSave.originalBadgeId) {
          toSave.originalBadgeId = toSave.badgeId;
        }
        toSave.startDate = toDate(toSave.startDate);
        toSave.endDate = toDate(toSave.endDate);
        saveBadge(toSave);
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
  return badge.value.numSkills > 0;
};

const getNoPublishMsg = () => {
  const msg = 'This Badge has no assigned Skills. A Badge cannot be published without at least one assigned Skill.';

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
      <template #subSubTitle v-if="badge && !isReadOnlyProj">
        <ButtonGroup v-if="!isReadOnlyProj">
          <SkillsButton @click="displayEditBadge"
                    ref="editBadgeButton"
                    class="btn btn-outline-primary"
                    size="small"
                    variant="outline-primary"
                    data-cy="btn_edit-badge"
                    id="editBadgeButton"
                    :track-for-focus="true"
                    label="Edit"
                    icon="fas fa-edit"
                    :aria-label="'edit Badge '+badge.badgeId">
          </SkillsButton>
          <SkillsButton v-if="badge.enabled !== 'true'"
                    @click.stop="handlePublish"
                    class="btn btn-outline-primary"
                    size="small"
                    id="badgeGoLiveButton"
                    :track-for-focus="true"
                    variant="outline-primary"
                    aria-label="Go Live"
                    label="Go Live"
                    data-cy="goLive">Go Live
          </SkillsButton>
        </ButtonGroup>
      </template>
    </page-header>

    <Message v-if="badge && badge.enabled !== 'true'"
             :closable="false"
             severity="warn">
      This badge cannot be achieved until it is live
    </Message>

    <navigation v-if="!isLoading" :nav-items="navItems"></navigation>
    <edit-badge v-if="showEditBadge" v-model="showEditBadge" :id="badge.badgeId" :badge="badge" :is-edit="true"
                :global="false" @badge-updated="badgeEdited" @hidden="handleHidden"></edit-badge>
  </div>
</template>

<style scoped></style>
