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
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import AdminGroupsService from '@/components/access/groups/AdminGroupsService.js';
import SkillsDropDown from '@/components/utils/inputForm/SkillsDropDown.vue';
import LoadingContainer from '@/components/utils/LoadingContainer.vue';
import Column from 'primevue/column';
import NoContent2 from '@/components/utils/NoContent2.vue';
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue';
import { useUserInfo } from '@/components/utils/UseUserInfo.js';
import { userErrorState } from '@/stores/UserErrorState.js';
import { useUpgradeInProgressErrorChecker } from '@/components/utils/errors/UseUpgradeInProgressErrorChecker.js';

const route = useRoute()
const userInfo = useUserInfo();
const errorState = userErrorState()
const upgradeInProgressErrorChecker = useUpgradeInProgressErrorChecker()

const isLoading = ref(true)
const availableProjects = ref([])
const assignedProjects = ref([])

const removeProjectInfo = ref({
  showDialog: false,
  project: {}
})

const adminGroupId = computed(() => route.params.adminGroupId)

const noProjectsAvailable = computed(() => {
  return availableProjects.value && availableProjects.value.length === 0;
})
const projectsAssigned = computed(() => {
  return availableProjects.value && availableProjects.value.length > 0;
})
const emptyMessage = computed(() => {
  if (!noProjectsAvailable.value) {
    return 'No results. Please refine your search string.'
  } else {
    if (projectsAssigned.value) {
      return 'You currently do not administer any projects.'
    }
    return 'All of your available projects have already been assigned to this admin group.'
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
  AdminGroupsService.getAdminGroupProjects(adminGroupId.value)
      .then((res) => {
        availableProjects.value = res.availableProjects;
        assignedProjects.value = res.assignedProjects;
      }).finally(() => {
    isLoading.value = false
  });
}
const addProjectToAdminGroup = (project) => {
  isLoading.value = true
  AdminGroupsService.addProjectToAdminGroup(adminGroupId.value, project.projectId).then((res) => {
    availableProjects.value = res.availableProjects;
    assignedProjects.value = res.assignedProjects;
  }).catch((e) => {
    handleError(e);
  }).finally(() => {
    isLoading.value = false
  });
}
const removeProjectFromAdminGroupConfirm = (project) => {
  removeProjectInfo.value.project = project
  removeProjectInfo.value.showDialog = true
}

const removeProjectFromAdminGroup = () => {
  isLoading.value = true
  const { projectId } = removeProjectInfo.value.project
  AdminGroupsService.removeProjectFromAdminGroup(adminGroupId.value, projectId)
      .then((res) => {
        availableProjects.value = res.availableProjects;
        assignedProjects.value = res.assignedProjects;
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
    errorState.navToErrorPage('Failed to add Project to Admin Group', errorMessage)
  }
}
const clearErrorMessage = () => {
  errNotification.value.msg = '';
  errNotification.value.enable = false;
}
</script>

<template>
  <sub-page-header title="Group Projects" />

  <Card :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }">
    <template #content>
      <loading-container :is-loading="isLoading" class="">
        <div class="w-full px-3 py-4">
          <SkillsDropDown
              aria-label="Select Project to add to Admin Group"
              name="associatedProject"
              data-cy="projectSelector"
              showClear
              filter
              optionLabel="name"
              @update:modelValue="addProjectToAdminGroup"
              :emptyMessage=emptyMessage
              :isRequired="true"
              :options="availableProjects">
            <template #value="slotProps">
              <div v-if="slotProps.value" class="p-1" :data-cy="`projectSelected-${slotProps.value.projectId}`">
               <span class="ml-1">{{ slotProps.value.name }}</span>
              </div>
              <span v-else> Search available projects...</span>
            </template>
            <template #option="slotProps">
              <div :data-cy="`availableProjectSelection-${slotProps.option.projectId}`">
                <span class="h6 ml-2">{{ slotProps.option.name }}</span>
              </div>
            </template>
          </SkillsDropDown>
        </div>
        <Message v-if="errNotification.enable" @close="clearErrorMessage" severity="error" data-cy="error-msg">
          <strong>Error!</strong> Request could not be completed! {{ errNotification.msg }}
        </Message>
        <div v-if="assignedProjects && assignedProjects.length > 0">
          <SkillsDataTable
              :loading="isLoading"
              tableStoredStateId="adminGroupProjectsTable"
              aria-label="Admin Group Projects"
              :value="assignedProjects"
              paginator
              :rows="5"
              :totalRecords="assignedProjects.length"
              :rowsPerPageOptions="[5, 10, 15, 20]"
              data-cy="adminGroupProjectsTable">
            <Column header="Name" field="name" style="width: 40%;" :sortable="true">
              <template #body="slotProps">
                <router-link :id="slotProps.data.projectId" :to="{ name:'Subjects',
                    params: { projectId: slotProps.data.projectId }}"
                             class="btn btn-sm btn-outline-hc ml-2" :data-cy="`manage_${slotProps.data.projectId}`">
                  {{ slotProps.data.name }}
                </router-link>
              </template>
            </Column>
            <Column header="Delete">
              <template #body="slotProps">
                <SkillsButton v-on:click="removeProjectFromAdminGroupConfirm(slotProps.data)" size="small"
                              :id="`removeProject_${slotProps.data.projectId}`"
                              :track-for-focus="true"
                              :data-cy="`removeProject_${slotProps.data.projectId}`" icon="fas fa-trash" label="Delete"
                              :aria-label="`remove project on ${slotProps.data.projectId} from admin group`">
                </SkillsButton>
              </template>
            </Column>

            <template #paginatorstart>
              <span>Total Rows:</span> <span class="font-semibold" data-cy=adminGroupProjectsTableTotalRows>{{ assignedProjects.length }}</span>
            </template>
          </SkillsDataTable>
        </div>

        <no-content2 v-else title="No Projects Added Yet..." icon="fas fa-spell-check" class="py-5"
                     message="Please use the drop-down above to start adding projects to this admin group!"></no-content2>
      </loading-container>
    </template>
  </Card>

  <RemovalValidation
      v-if="removeProjectInfo.showDialog"
      v-model="removeProjectInfo.showDialog"
      @do-remove="removeProjectFromAdminGroup"
      :item-name="removeProjectInfo.project.name"
      removalTextPrefix="This will remove the "
      :item-type="`project from this admin group.  All members of this admin group other than ${userInfo.userInfo.value.userId} will lose admin access to this project`"
      :enable-return-focus="true">
  </RemovalValidation>
</template>

<style scoped>

</style>