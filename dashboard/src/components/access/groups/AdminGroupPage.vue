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

import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router'
import { useFocusState } from '@/stores/UseFocusState.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import PageHeader from '@/components/utils/pages/PageHeader.vue';
import Navigation from '@/components/utils/Navigation.vue';
import { useAdminGroupState } from '@/stores/UseAdminGroupState.js';
import EditAdminGroup from '@/components/access/groups/EditAdminGroup.vue';
import Avatar from 'primevue/avatar';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';

const announcer = useSkillsAnnouncer()
const router = useRouter()
const route = useRoute()
const adminGroupState = useAdminGroupState()
const appConfig = useAppConfig()
const focusState = useFocusState()

onMounted(() => {
  if (!adminGroupState.adminGroup || adminGroupState.adminGroup.adminGroupId !== route.params.adminGroupId) {
    adminGroupState.loadAdminGroup(route.params.adminGroupId).then((adminGroup) => {
      editAdminGroupInfo.value.adminGroupDef = adminGroup
    })
  } else {
    editAdminGroupInfo.value.adminGroupDef = adminGroupState.adminGroup
  }
})

const isLoading = computed(() => adminGroupState.loadingAdminGroup)
const navItems = computed(() => {
  const res = [
    { name: 'Members', iconClass: 'fa-users skills-color-users', page: 'AdminGroupMembers' },
    { name: 'Projects', iconClass: 'fa-tasks skills-color-projects', page: 'AdminGroupProjects' },
    { name: 'Quizzes and Surveys', iconClass: 'fa-spell-check skills-color-subjects', page: 'AdminGroupQuizzes' },
    { name: 'Global Badges', iconClass: 'fa-globe-americas skills-color-badges', page: 'AdminGroupGlobalBadges' },
  ];

  return res;
})
const headerOptions = computed(() => {
  const adminGroup = adminGroupState.adminGroup
  if (!adminGroup) {
    return {};
  }
  return {
    icon: 'fas fa-users skills-color-access',
    title: `Admin Group: ${adminGroup.name}`,
    subTitle: `ID: ${adminGroup.adminGroupId}`,
    stats: [{
      label: 'Members',
      count: (adminGroup.numberOfMembers + adminGroup.numberOfOwners),
      icon: 'fas fa-users skills-color-users',
    }, {
      label: 'Projects',
      count: adminGroup.numberOfProjects,
      icon: 'fas fa-tasks skills-color-projects',
    }, {
      label: 'Quizzes and Surveys',
      count: adminGroup.numberOfQuizzesAndSurveys,
      icon: 'fas fa-spell-check skills-color-subjects',
    }, {
      label: 'Global Badges',
      count: adminGroup.numberOfGlobalBadges,
      icon: 'fas fa-globe-americas skills-color-badges',
    }],
  };
})
const editAdminGroupInfo = ref({
  showDialog: false,
  isEdit: true,
  adminGroupDef: {},
});
function updateEditAdminGroupInfo(adminGroupSummary) {
  editAdminGroupInfo.value.adminGroupDef.name = adminGroupSummary.name
  editAdminGroupInfo.value.adminGroupDef.userCommunity = adminGroupSummary.userCommunity
}
function updateAdminGroupDef(adminGroup) {
  focusState.focusOnLastElement()
  updateEditAdminGroupInfo(adminGroup)
  adminGroupState.adminGroup.name = adminGroup.name
  adminGroupState.adminGroup.userCommunity = adminGroup.userCommunity
  announcer.polite(`Admin Group named ${adminGroup.name} was saved`);
}
</script>

<template>
  <div>
    <PageHeader :loading="isLoading" :options="headerOptions">
      <template #subTitle v-if="adminGroupState.adminGroup">
        <div v-if="adminGroupState.adminGroup.userCommunity" class="mb-4" data-cy="userCommunity">
          <Avatar icon="fas fa-shield-alt" class="text-red-500"></Avatar>
          <span class="text-muted-color italic ml-1">{{ appConfig.userCommunityBeforeLabel }}</span> <span
            class="font-bold text-primary">{{ adminGroupState.adminGroup.userCommunity }}</span> <span
            class="font-bold text-primary">{{ appConfig.userCommunityAfterLabel }}</span>
        </div>
      </template>
      <template #subSubTitle v-if="adminGroupState.adminGroup">
        <div>
          <div class="mt-2">
            <SkillsButton @click="editAdminGroupInfo.showDialog = true"
                          label="Edit"
                          icon="fas fa-edit"
                          size="small"
                          severity="info"
                          outlined
                          data-cy="editAdminGroupButton"
                          :aria-label="`Edit Admin Group ${adminGroupState.adminGroup.name}`"
                          ref="editAdminGroupButton"
                          id="editAdminGroupButton"
                          :track-for-focus="true"
                          title="Edit Admin Group">
            </SkillsButton>
          </div>
        </div>
      </template>
    </PageHeader>

    <EditAdminGroup
        v-if="editAdminGroupInfo.showDialog"
        v-model="editAdminGroupInfo.showDialog"
        :admin-group="editAdminGroupInfo.adminGroupDef"
        :is-edit="editAdminGroupInfo.isEdit"
        @admin-group-saved="updateAdminGroupDef"
        :enable-return-focus="true"/>

    <Navigation :nav-items="navItems">
    </Navigation>

  </div>
</template>

<style scoped>

</style>