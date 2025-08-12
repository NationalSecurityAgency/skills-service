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

import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router';
import { useUserInfo } from '@/components/utils/UseUserInfo.js';
import { useAdminGroupState } from '@/stores/UseAdminGroupState.js';
import { userErrorState } from '@/stores/UserErrorState.js';
import { useUpgradeInProgressErrorChecker } from '@/components/utils/errors/UseUpgradeInProgressErrorChecker.js';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import AdminGroupsService from '@/components/access/groups/AdminGroupsService.js';
import SkillsDropDown from '@/components/utils/inputForm/SkillsDropDown.vue';
import LoadingContainer from '@/components/utils/LoadingContainer.vue';
import Column from 'primevue/column';
import NoContent2 from '@/components/utils/NoContent2.vue';
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue';

const route = useRoute()
const userInfo = useUserInfo();
const adminGroupState = useAdminGroupState()
const errorState = userErrorState()
const upgradeInProgressErrorChecker = useUpgradeInProgressErrorChecker()

const isLoading = ref(true)
const availableGlobalBadges = ref([])
const assignedGlobalBadges = ref([])

const removeGlobalBadgeInfo = ref({
  showDialog: false,
  globalBadge: {}
})

const adminGroupId = computed(() => route.params.adminGroupId)

const areGlobalBadgesAvailable = computed(() => {
  return availableGlobalBadges.value && availableGlobalBadges.value.length > 0;
})
const areGlobalBadgesAssigned = computed(() => {
  return assignedGlobalBadges.value && assignedGlobalBadges.value.length > 0;
})
const emptyMessage = computed(() => {
  if (areGlobalBadgesAvailable.value) {
    return 'No results. Please refine your search string.'
  } else {
    if (areGlobalBadgesAssigned.value) {
      return 'All of your available global badges have already been assigned to this admin group.'
    }
    return 'You currently do not administer any global badges.'
  }
})
const errNotification = ref({
  enable: false,
  msg: '',
});

onMounted(() => {
  loadData()
})
const loadData = () => {
  isLoading.value = true
  AdminGroupsService.getAdminGroupGlobalBadges(adminGroupId.value)
      .then((res) => {
        availableGlobalBadges.value = res.availableGlobalBadges;
        assignedGlobalBadges.value = res.assignedGlobalBadges;
      }).finally(() => {
    isLoading.value = false
  });
}
const addGlobalBadgeToAdminGroup = (globalBadge) => {
  isLoading.value = true
  AdminGroupsService.addGlobalBadgeToAdminGroup(adminGroupId.value, globalBadge.badgeId)
    .then((res) => {
      availableGlobalBadges.value = res.availableGlobalBadges;
      assignedGlobalBadges.value = res.assignedGlobalBadges;
      adminGroupState.adminGroup.numberOfGlobalBadges++;
    }).catch((e) => {
      handleError(e);
    }).finally(() => {
      isLoading.value = false
    });
}
const removeGlobalBadgeFromAdminGroupConfirm = (globalBadge) => {
  removeGlobalBadgeInfo.value.globalBadge = globalBadge
  removeGlobalBadgeInfo.value.showDialog = true
}

const removeGlobalBadgeFromAdminGroup = () => {
  isLoading.value = true
  const { badgeId } = removeGlobalBadgeInfo.value.globalBadge
  AdminGroupsService.removeGlobalBadgeFromAdminGroup(adminGroupId.value, badgeId)
      .then((res) => {
        availableGlobalBadges.value = res.availableGlobalBadges;
        assignedGlobalBadges.value = res.assignedGlobalBadges;
        console.log(`adminGroupState.adminGroup.numberOfGlobalBadges: ${adminGroupState.adminGroup.numberOfGlobalBadges}`)
        adminGroupState.adminGroup.numberOfGlobalBadges--;
        console.log(`done. adminGroupState.adminGroup.numberOfGlobalBadges: ${adminGroupState.adminGroup.numberOfGlobalBadges}`)
      }).finally(() => {
    isLoading.value = false
  });
}
const handleError = (e) => {
  if (e.response.data && e.response.data.errorCode && e.response.data.errorCode === 'AccessDenied') {
    errNotification.value.msg = e.response.data.explanation;
    errNotification.value.enable = true;
  } else if (upgradeInProgressErrorChecker.isUpgrading(e)) {
    upgradeInProgressErrorChecker.navToUpgradeInProgressPage()
  } else {
    const errorMessage = (e.response && e.response.data && e.response.data.message) ? e.response.data.message : undefined;
    errorState.navToErrorPage('Failed to add GlobalBadge to Admin Group', errorMessage)
  }
}
const clearErrorMessage = () => {
  errNotification.value.msg = '';
  errNotification.value.enable = false;
}
</script>

<template>
  <sub-page-header title="Group Global Badges" />

  <Card :pt="{ body: { class: 'p-0!' } }">
    <template #content>
      <loading-container :is-loading="isLoading" class="">
        <div class="w-full px-4 py-6">
          <SkillsDropDown
              name="associatedGlobalBadge"
              data-cy="globalBadgeSelector"
              aria-label="Select Global Badge to add to Admin Group"
              showClear
              filter
              optionLabel="name"
              @update:modelValue="addGlobalBadgeToAdminGroup"
              :emptyMessage=emptyMessage
              :isRequired="true"
              :options="availableGlobalBadges">
            <template #value="slotProps">
              <div v-if="slotProps.value" class="p-1" :data-cy="`globalBadgeSelected-${slotProps.value.badgeId}`">
                <span class="ml-1">{{ slotProps.value.name }}</span>
              </div>
              <span v-else> Search available global badges...</span>
            </template>
            <template #option="slotProps">
              <div :data-cy="`availableGlobalBadgeSelection-${slotProps.option.badgeId}`">
                <span class="h6 ml-2">{{ slotProps.option.name }}</span>
              </div>
            </template>
          </SkillsDropDown>
        </div>
        <Message v-if="errNotification.enable" @close="clearErrorMessage" severity="error" data-cy="error-msg">
          <strong>Error!</strong> Request could not be completed! {{ errNotification.msg }}
        </Message>
        <div v-if="assignedGlobalBadges && assignedGlobalBadges.length > 0">
          <SkillsDataTable
              :loading="isLoading"
              tableStoredStateId="adminGroupGlobalBadgesTable"
              aria-label="Admin Group Global Badges Table"
              :value="assignedGlobalBadges"
              paginator
              :rows="5"
              :totalRecords="assignedGlobalBadges.length"
              :rowsPerPageOptions="[5, 10, 15, 20]"
              data-cy="adminGroupGlobalBadgesTable">
            <Column header="Name" field="name" style="width: 40%;" :sortable="true">
              <template #body="slotProps">
                <router-link :id="slotProps.data.badgeId" :to="{ name:'GlobalBadgeSkills',
                    params: { badgeId: slotProps.data.badgeId }}"
                             class="btn btn-sm btn-outline-hc ml-2" :data-cy="`manage_${slotProps.data.badgeId}`">
                  {{ slotProps.data.name }}
                </router-link>
              </template>
            </Column>
            <Column header="Delete">
              <template #body="slotProps">
                <SkillsButton v-on:click="removeGlobalBadgeFromAdminGroupConfirm(slotProps.data)" size="small"
                              :id="`removeGlobalBadge_${slotProps.data.badgeId}`"
                              :track-for-focus="true"
                              :data-cy="`removeGlobalBadge_${slotProps.data.badgeId}`" icon="fas fa-trash" label="Delete"
                              :aria-label="`remove global badge on ${slotProps.data.badgeId} from admin group`">
                </SkillsButton>
              </template>
            </Column>

            <template #paginatorstart>
              <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ assignedGlobalBadges.length }}</span>
            </template>
          </SkillsDataTable>
        </div>

        <no-content2 v-else title="No Global Badges Added Yet..." icon="fas fa-spell-check" class="py-8">
          <div>
            <p>
              Please use the drop-down above to start adding global badges to this admin group!
            </p>
            <p>
              When a global badge is assigned to a group, group's members automatically gain administrative privileges of that badge, streamlining management.
            </p>
          </div>
        </no-content2>
      </loading-container>
    </template>
  </Card>

  <RemovalValidation
      v-if="removeGlobalBadgeInfo.showDialog"
      v-model="removeGlobalBadgeInfo.showDialog"
      @do-remove="removeGlobalBadgeFromAdminGroup"
      :item-name="removeGlobalBadgeInfo.globalBadge.name"
      removalTextPrefix="This will remove the "
      :item-type="`global badge from this admin group.  All members of this admin group other than ${userInfo.userInfo.value.userIdForDisplay} will lose admin access to this global badge`"
      :enable-return-focus="true">
  </RemovalValidation>
</template>

<style scoped>

</style>