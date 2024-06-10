<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Navigation from "@/components/utils/Navigation.vue";
import EditBadge from "@/components/badges/EditBadge.vue";
import PageHeader from "@/components/utils/pages/PageHeader.vue";
import GlobalBadgeService from "@/components/badges/global/GlobalBadgeService.js";
import {useBadgeState} from "@/stores/UseBadgeState.js";
import {storeToRefs} from "pinia";

const route = useRoute();
const router = useRouter();

const isLoading = ref(true);
const badgeId = ref(route.params.badgeId);
const showEdit = ref(false);
const badgeState = useBadgeState();
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

const badgeEdited = (editedBadge) => {
  GlobalBadgeService.saveBadge(editedBadge).then((resp) => {
    const origId = badge.badgeId;
    badgeState.value = editedBadge;
    if (origId !== resp.badgeId) {
      router.replace({ name: route.name, params: { ...route.params, badgeId: resp.badgeId } });
      badgeId.value = resp.badgeId;
    }
  }).finally(() => {
    handleFocus();
  });
};

const handleHidden = (e) => {
  if (!e || !e.saved) {
    handleFocus();
  }
};

const handleFocus = () => {
  nextTick(() => {
    // const ref = $refs.editBadgeButton;
    // if (ref) {
    //   ref.focus();
    // }
  });
};

const handlePublish = () => {
  if (canPublish()) {
    const msg = `While this Badge is disabled, user's cannot see the Badge or achieve it. Once the Badge is live, it will be visible to users.
        Please note that once the badge is live, it cannot be disabled.`;
    // msgConfirm(msg, 'Please Confirm!', 'Yes, Go Live!')
    //     .then((res) => {
    //       if (res) {
    //         badge.enabled = 'true';
    //         const toSave = { ...badge };
    //         if (!toSave.originalBadgeId) {
    //           toSave.originalBadgeId = toSave.badgeId;
    //         }
    //         toSave.startDate = toDate(toSave.startDate);
    //         toSave.endDate = toDate(toSave.endDate);
    //         badgeEdited(toSave);
    //       }
    //     });
  } else {
    // msgOk(getNoPublishMsg(), 'Empty Badge!');
  }
};

const canPublish = () => {
  return badge.numSkills > 0 || badge.requiredProjectLevels.length > 0;
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
<!--        <b-button-group class="mb-3" size="sm">-->
        <ButtonGroup>
          <SkillsButton @click="displayEditBadge"
                    ref="editBadgeButton"
                    class="btn btn-outline-primary"
                    size="small"
                    data-cy="btn_edit-badge"
                    :aria-label="'edit Badge '+badge.badgeId"
                    label="Edit"
                    icon="fas fa-edit">
          </SkillsButton>
          <SkillsButton v-if="badge.enabled !== 'true'"
                    @click.stop="handlePublish"
                    class="btn btn-outline-primary"
                    size="small"
                    aria-label="Go Live"
                    data-cy="goLive" label="Go Live">
          </SkillsButton>
        </ButtonGroup>
<!--        </b-button-group>-->
      </template>
    </page-header>

    <navigation :nav-items="[
          {name: 'Skills', iconClass: 'fa-graduation-cap skills-color-skills', page: 'GlobalBadgeSkills'},
          {name: 'Levels', iconClass: 'fa-trophy skills-color-levels', page: 'GlobalBadgeLevels'},
        ]">
    </navigation>
    <edit-badge v-if="showEdit" v-model="showEdit" :id="badge.badgeId" :badge="badge" :is-edit="true"
                :global="true" @badge-updated="badgeEdited" @hidden="handleHidden"></edit-badge>
  </div>
</template>

<style scoped>

</style>